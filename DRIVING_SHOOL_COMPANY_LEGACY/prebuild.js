// @ts-nocheck
#!/usr/bin/env node
/**
 * Pre-build safety net (runs automatically before `next build` via the
 * "prebuild" npm hook in package.json).
 *
 * cPanel's File Manager extractor can silently drop the components/ folder
 * from uploaded archives. If components/ui/card.tsx is missing, this script
 * restores it by extracting just ./components from a deploy archive that is
 * sitting in the app root (backend-deploy.tar.gz or components-fix.tar.gz)
 * using the server's own tar — which never fails.
 */
const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

process.chdir(__dirname);

/**
 * Windows-only patch for a Next.js webpack bug (vercel/next.js#96823):
 * on Windows, pluginState.serverActionModules ends up empty while
 * serverActions has workers, so createActionAssets crashes with
 * "Cannot read properties of undefined (reading 'client')" during
 * next build (the same code builds green on Linux/macOS). The fix
 * tolerates an unresolvable action worker — a no-op on Linux where
 * the map is always populated. Runs before every local `npm run build`
 * and re-applies itself if node_modules is reinstalled.
 */
function patchNextFlightPluginForWindows() {
  if (process.platform !== 'win32') return;

  const pluginPath = path.join(
    __dirname,
    'node_modules', 'next', 'dist', 'build', 'webpack', 'plugins',
    'flight-client-entry-plugin.js'
  );

  if (!fs.existsSync(pluginPath)) {
    console.log('[prebuild] (win32) next flight plugin not found — skipping Windows patch');
    return;
  }

  let src = fs.readFileSync(pluginPath, 'utf8');

  if (src.includes('prebuild-windows-guard')) {
    console.log('[prebuild] (win32) next flight plugin already patched ✓');
    return;
  }

  const guard = (mapExpr) =>
    `const _sa = ${mapExpr} || {}; // prebuild-windows-guard\n` +
    `                const modId = _sa[action.layer[name] === _constants.WEBPACK_LAYERS.actionBrowser ? 'client' : 'server'];`;

  const replacements = [
    [
      /const modId = pluginState\.serverActionModules\[name\]\[action\.layer\[name\] === _constants\.WEBPACK_LAYERS\.actionBrowser \? 'client' : 'server'\];/,
      guard('pluginState.serverActionModules[name]')
    ],
    [
      /const modId = pluginState\.edgeServerActionModules\[name\]\[action\.layer\[name\] === _constants\.WEBPACK_LAYERS\.actionBrowser \? 'client' : 'server'\];/,
      guard('pluginState.edgeServerActionModules[name]')
    ]
  ];

  let applied = 0;
  for (const [re, replacement] of replacements) {
    if (src.match(re)) {
      src = src.replace(re, () => replacement);
      applied++;
    }
  }

  if (applied !== replacements.length) {
    console.error('[prebuild] (win32) could not locate all flight plugin lookup sites (' + applied + '/' + replacements.length + ') — leaving node_modules untouched.');
    return;
  }

  fs.writeFileSync(pluginPath, src, 'utf8');
  console.log('[prebuild] (win32) patched next flight-client-entry-plugin (Windows server-action guard) ✓');
}

/**
 * Windows-only patch for a Next.js file-tracing crash: @vercel/nft resolves
 * modules that reference os.homedir() by globbing <homedir>/**\/* (the Prisma
 * runtime bundles dotenv's tilde expansion, which is in every server chunk).
 * On Windows the user profile contains junction points (e.g. C:\Users\HP\Cookies,
 * C:\Users\HP\Application Data) that throw EPERM on scandir, and node-glob's
 * strict mode turns that into a fatal build error. On Linux the same glob runs
 * against an accessible home dir and completes fine, so treating EPERM/EACCES
 * directories as unreadable (skippable) is a no-op on the server.
 */
function patchNextCompiledGlobForWindows() {
  if (process.platform !== 'win32') return;

  const globPath = path.join(
    __dirname,
    'node_modules', 'next', 'dist', 'compiled', 'glob', 'glob.js'
  );

  if (!fs.existsSync(globPath)) {
    console.log('[prebuild] (win32) next compiled glob not found — skipping glob patch');
    return;
  }

  let src = fs.readFileSync(globPath, 'utf8');

  if (src.includes('prebuild-windows-glob-guard')) {
    console.log('[prebuild] (win32) next compiled glob already patched ✓');
    return;
  }

  const replacements = [
    [
      /if\(this\.strict\)\{this\.emit\("error",e\);this\.abort\(\)\}if\(!this\.silent\)console\.error\("glob error",e\);break/,
      'if(this.strict&&e.code!=="EPERM"&&e.code!=="EACCES"){this.emit("error",e);this.abort()}/*prebuild-windows-glob-guard*/if(!this.silent)console.error("glob error",e);break'
    ],
    [
      /if\(this\.strict\)throw e;if\(!this\.silent\)console\.error\("glob error",e\);break/,
      'if(this.strict&&e.code!=="EPERM"&&e.code!=="EACCES")throw e;/*prebuild-windows-glob-guard*/if(!this.silent)console.error("glob error",e);break'
    ]
  ];

  let applied = 0;
  for (const [re, replacement] of replacements) {
    if (src.match(re)) {
      src = src.replace(re, () => replacement);
      applied++;
    }
  }

  if (applied !== replacements.length) {
    console.error('[prebuild] (win32) could not locate all compiled glob strict sites (' + applied + '/' + replacements.length + ') — leaving node_modules untouched.');
    return;
  }

  fs.writeFileSync(globPath, src, 'utf8');
  console.log('[prebuild] (win32) patched next compiled glob (EPERM-tolerant directory scan) ✓');
}

/**
 * Windows-only patch for @vercel/nft: when a traced module references
 * os.homedir() (the Prisma runtime bundles dotenv's tilde expansion, present in
 * every server chunk), nft globs <homedir>/**\/* to resolve it. On Windows that
 * walks the entire user profile (junction loops, tens of thousands of files) and
 * either crashes on EPERM or exhausts memory. The home directory is never part
 * of the app's trace output, so skipping those globs is safe — and a no-op on
 * Linux, where this exact glob runs against an accessible home dir and completes.
 */
function patchNftHomeGlobForWindows() {
  if (process.platform !== 'win32') return;

  const nftPath = path.join(
    __dirname,
    'node_modules', 'next', 'dist', 'compiled', '@vercel', 'nft', 'index.js'
  );

  if (!fs.existsSync(nftPath)) {
    console.log('[prebuild] (win32) @vercel/nft not found — skipping nft patch');
    return;
  }

  let src = fs.readFileSync(nftPath, 'utf8');

  if (src.includes('prebuild-windows-nft-guard')) {
    console.log('[prebuild] (win32) @vercel/nft already patched ✓');
    return;
  }

  const anchor = 'const u=e.substring(0,o);const f=e.slice(o);';
  if (!src.includes(anchor)) {
    console.error('[prebuild] (win32) could not locate nft emitAssetDirectory anchor — leaving node_modules untouched.');
    return;
  }

  const injected =
    'const u=e.substring(0,o);const _hd=(process.env.USERPROFILE||"").replace(/\\\\/g,"/").toLowerCase();if(_hd&&u.replace(/\\\\/g,"/").toLowerCase().startsWith(_hd))return;/*prebuild-windows-nft-guard*/const f=e.slice(o);';

  fs.writeFileSync(nftPath, src.replace(anchor, injected), 'utf8');
  console.log('[prebuild] (win32) patched @vercel/nft (skip home-directory globs) ✓');
}

patchNextFlightPluginForWindows();
patchNextCompiledGlobForWindows();
patchNftHomeGlobForWindows();

// --- Ensure tsconfig.json has @/* path alias ----------------------------------
// The webpack config function in next.config.mjs can't be serialized when
// workerThreads is true (DataCloneError), so we remove the webpack alias and
// instead rely on Next.js's native tsconfig path resolution.
function ensureTsconfigAlias() {
  const tsconfigPath = path.join(__dirname, 'tsconfig.json');
  if (!fs.existsSync(tsconfigPath)) {
    console.log('[prebuild] tsconfig.json not found — skipping @/* alias');
    return;
  }
  let ts;
  try {
    ts = JSON.parse(fs.readFileSync(tsconfigPath, 'utf8'));
  } catch (e) {
    console.log('[prebuild] tsconfig.json parse error: ' + e.message);
    return;
  }
  if (!ts.compilerOptions) ts.compilerOptions = {};
  if (!ts.compilerOptions.paths) ts.compilerOptions.paths = {};
  if (ts.compilerOptions.paths['@/*']) {
    console.log('[prebuild] tsconfig @/* paths already configured ✓');
    return;
  }
  ts.compilerOptions.paths['@/*'] = ['./*'];
  if (!ts.compilerOptions.baseUrl) ts.compilerOptions.baseUrl = '.';
  fs.writeFileSync(tsconfigPath, JSON.stringify(ts, null, 2) + '\n', 'utf8');
  console.log('[prebuild] patched tsconfig.json with @/* paths ✓');
}
ensureTsconfigAlias();

// --- Force all routes dynamic to avoid EAGAIN worker crash on shared hosts -
// On cPanel shared hosting with tight nproc limits, Next.js cannot spawn the
// worker thread needed for static page generation. Adding
//   export const dynamic = 'force-dynamic'
// to the root app/layout.tsx tells Next.js to render ALL routes at request
// time, completely skipping static generation — and the worker.
function ensureForceDynamicLayout() {
  const appDir = path.join(__dirname, 'app');
  if (!fs.existsSync(appDir)) {
    console.log('[prebuild] app/ directory not found — skipping force-dynamic layout');
    return;
  }
  // Check all possible layout file extensions
  const layoutExts = ['tsx', 'ts', 'jsx', 'js'];
  let layoutFile = null;
  for (const ext of layoutExts) {
    const candidate = path.join(appDir, 'layout.' + ext);
    if (fs.existsSync(candidate)) { layoutFile = candidate; break; }
  }

  if (layoutFile) {
    let src = fs.readFileSync(layoutFile, 'utf8');
    if (src.includes('force-dynamic')) {
      console.log('[prebuild] root layout already has force-dynamic ✓');
      return;
    }
    // Prepend the dynamic export right after any 'use client' directive
    const useClientRe = /^(\s*['"]use client['"]\s*;?\s*)/m;
    const m = src.match(useClientRe);
    if (m) {
      src = src.replace(useClientRe, m[1] + "\nexport const dynamic = 'force-dynamic';\n");
    } else {
      src = "export const dynamic = 'force-dynamic';\n\n" + src;
    }
    fs.writeFileSync(layoutFile, src, 'utf8');
    console.log('[prebuild] patched root layout with force-dynamic ✓');
  } else {
    // No layout exists — create a minimal one
    const content = "export const dynamic = 'force-dynamic';\n\n" +
      'export default function RootLayout({ children }) {\n' +
      '  return children;\n' +
      '}\n';
    fs.writeFileSync(path.join(appDir, 'layout.tsx'), content, 'utf8');
    console.log('[prebuild] created root layout.tsx with force-dynamic ✓');
  }
}
ensureForceDynamicLayout();

// --- Force-dynamic for Pages Router pages (if any exist on the server) ------
// Scan pages/ directory and inject getServerSideProps into any page that
// doesn't already have one, so no page triggers static generation.
function ensurePagesRouterDynamic() {
  const pagesDir = path.join(__dirname, 'pages');
  if (!fs.existsSync(pagesDir)) {
    console.log('[prebuild] pages/ directory not found — skipping Pages Router patch');
    return;
  }
  function walk(dir) {
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    for (const e of entries) {
      const full = path.join(dir, e.name);
      if (e.isDirectory()) { walk(full); continue; }
      if (!/\.(tsx?|jsx?)$/.test(e.name)) continue;
      // Skip internal Next.js files and API routes
      if (/_app\.|_document\.|_error\.|_not-found\.|api\//.test(full)) continue;
      let src = fs.readFileSync(full, 'utf8');
      if (src.includes('getServerSideProps')) continue;
      // If it has getStaticProps, rename it to getServerSideProps
      if (src.includes('getStaticProps')) {
        src = src.replace(/getStaticProps/g, 'getServerSideProps');
        // Also remove getStaticPaths (not needed with getServerSideProps)
        if (src.includes('getStaticPaths')) {
          src = src.replace(/export\s+(const|async\s+function)\s+getStaticPaths/g,
            '// @ts-ignore — removed by prebuild\nconst _removed_getStaticPaths');
        }
        fs.writeFileSync(full, src, 'utf8');
        console.log('[prebuild] patched ' + path.relative(__dirname, full) + ' — replaced getStaticProps → getServerSideProps');
      } else {
        // No data-fetching method — inject a minimal getServerSideProps
        src += '\n\nexport async function getServerSideProps() { return { props: {} }; }\n';
        fs.writeFileSync(full, src, 'utf8');
        console.log('[prebuild] patched ' + path.relative(__dirname, full) + ' — added getServerSideProps');
      }
    }
  }
  walk(pagesDir);
}
ensurePagesRouterDynamic();

// --- Patch next build to skip worker thread creation on shared hosts ----------
// On cPanel shared hosting with tight nproc/thread limits, the jest-worker
// pool inside next build cannot spawn even a single worker thread
// (ERR_WORKER_INIT_FAILED / EAGAIN). This patches next/dist/build/index.js
// to replace the real worker with an in-process mock that tells Next.js
// "every page is dynamic, nothing is static" — so no static generation
// worker is ever created.
function patchBuildSkipWorker() {
  const buildIdx = path.join(__dirname, 'node_modules', 'next', 'dist', 'build', 'index.js');
  if (!fs.existsSync(buildIdx)) {
    console.log('[prebuild] next/dist/build/index.js not found — skipping worker patch');
    return;
  }
  let src = fs.readFileSync(buildIdx, 'utf8');
  // Already patched?
  if (src.includes('/*shared-hosting-no-worker*/')) {
    console.log('[prebuild] next build already patched (no-worker) ✓');
    return;
  }
  // Locate the createStaticWorker call — it uses variable indentation so match
  // the pattern flexibly.
  const workerCallRe = /(\s*)const worker = createStaticWorker\(config, \{\s*debuggerPortOffset: -1\s*\}\s*\);/;
  const m = src.match(workerCallRe);
  if (!m) {
    console.log('[prebuild] could not locate createStaticWorker call — skipping worker patch');
    return;
  }
  const indent = m[1]; // e.g. '            '
  const mock = [
    indent + '/*shared-hosting-no-worker*/',
    indent + 'const worker = {',
    indent + '  isPageStatic: async () => ({ isStatic: false, hasStaticProps: false, hasServerProps: false, prerenderedRoutes: null, prerenderFallbackMode: null, appConfig: { dynamic: "force-dynamic" } }),',
    indent + '  hasCustomGetInitialProps: async () => false,',
    indent + '  getDefinedNamedExports: async () => [],',
    indent + '  end: async () => ({ forceExited: false }),',
    indent + '};',
  ].join('\n');
  src = src.replace(workerCallRe, mock);
  fs.writeFileSync(buildIdx, src, 'utf8');
  console.log('[prebuild] patched next build — worker replaced with in-process mock ✓');
}
patchBuildSkipWorker();

const cardTsx = path.join(__dirname, 'components', 'ui', 'card.tsx');

if (fs.existsSync(cardTsx)) {
  console.log('[prebuild] components/ui/card.tsx present — nothing to do ✓');
  // Diagnostic: tell us whether the server's tsconfig actually has the @/* paths
  // (a stale tsconfig without paths is what broke resolution before).
  try {
    const ts = JSON.parse(fs.readFileSync(path.join(__dirname, 'tsconfig.json'), 'utf8'));
    const p = ts.compilerOptions && ts.compilerOptions.paths;
    const ok = p && Array.isArray(p['@/*']);
    console.log('[prebuild] tsconfig @/* paths: ' + (ok ? 'CONFIGURED ✓' : 'MISSING \u2014 prebuild will inject this'));
  } catch (e) {
    console.log('[prebuild] tsconfig.json unreadable: ' + e.message);
  }
  process.exit(0);
}

const archives = ['backend-deploy.tar.gz', 'components-fix.tar.gz'];
let used = null;
for (const a of archives) {
  if (fs.existsSync(path.join(__dirname, a))) { used = a; break; }
}

if (!used) {
  console.error('[prebuild] FATAL: components/ui/card.tsx is missing AND no archive');
  console.error('  (' + archives.join(' or ') + ') was found in ' + __dirname);
  console.error('  → Upload the deploy archive to this folder and re-run.');
  process.exit(1);
}

console.log('[prebuild] components/ui missing — extracting ./components from ' + used + ' ...');
execSync(`tar -xzf "${used}" -C . ./components`, { stdio: 'inherit' });

if (fs.existsSync(cardTsx)) {
  console.log('[prebuild] components/ui restored ✓ — continuing build.');
  process.exit(0);
}

console.error('[prebuild] FAILED to restore components/ui — build will fail.');
process.exit(1);
