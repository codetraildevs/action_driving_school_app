import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"
import { existsSync, mkdirSync, writeFileSync } from 'fs';
import { join } from 'path';

interface ThumbnailOptions {
  width?: number;
  height?: number;
  quality?: number; // Ignored for SVG
  format?: 'svg'; // Force SVG to avoid child processes
}

export async function generateThumbnail(
  filePath: string,
  fileType: string,
  title: string,
  filename: string,
  options: ThumbnailOptions = {}
): Promise<string> {
  const {
    width = 300,
    height = 200,
    format = 'svg' // Always use SVG
  } = options;

  const outputDir = join(process.cwd(), "public", "uploads", "learning-materials", "thumbnails");

  if (!existsSync(outputDir)) {
    mkdirSync(outputDir, { recursive: true });
  }

  const outputPath = join(outputDir, `${filename}.svg`);

  try {
    // For all file types, generate SVG thumbnails
    await generateFileTypePlaceholder(fileType, outputPath, width, height, title);
    
    return "/uploads/learning-materials/thumbnails/" + `${filename}.svg`;

  } catch (error) {
    console.error(`Thumbnail generation failed for ${fileType}:`, error);
    
    const fallbackPath = join(outputDir, `${filename}-fallback.svg`);
    await generateFileTypePlaceholder(fileType, fallbackPath, width, height, title);
    
    return "/uploads/learning-materials/thumbnails/" + `${filename}-fallback.svg`;
  }
}

// Enhanced SVG placeholder generator with better visuals
async function generateFileTypePlaceholder(
  fileType: string,
  outputPath: string,
  width: number,
  height: number,
  title: string
): Promise<void> {
  const typeConfigs: { [key: string]: { color: string; icon: string; label: string } } = {
    'pdf': { color: '#FF6B6B', icon: '📄', label: 'PDF' },
    'document': { color: '#42A5F5', icon: '📝', label: 'Document' },
    'presentation': { color: '#FFA726', icon: '📊', label: 'Presentation' },
    'spreadsheet': { color: '#4CAF50', icon: '📈', label: 'Spreadsheet' },
    'video': { color: '#AB47BC', icon: '🎬', label: 'Video' },
    'audio': { color: '#66BB6A', icon: '🎵', label: 'Audio' },
    'image': { color: '#EC407A', icon: '🖼️', label: 'Image' },
    'archive': { color: '#78909C', icon: '📦', label: 'Archive' },
    'code': { color: '#5C6BC0', icon: '💻', label: 'Code' },
    'default': { color: '#607D8B', icon: '📁', label: 'File' }
  };

  const fileCategory = getFileCategory(fileType);
  const config = typeConfigs[fileCategory] || typeConfigs.default;
  const truncatedTitle = truncateText(title, 25);

  const svgContent = generateEnhancedSVG(config, width, height, truncatedTitle);
  writeFileSync(outputPath, svgContent);
}

// Generate enhanced SVG with shadows and gradients
function generateEnhancedSVG(
  config: { color: string; icon: string; label: string },
  width: number,
  height: number,
  title: string
): string {
  const darkColor = darkenColor(config.color, 20);
  const fontSize = Math.min(width, height) * 0.06;
  const iconSize = Math.min(width, height) * 0.2;
  
  return `
    <svg width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id="bgGradient" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="${config.color}" />
          <stop offset="100%" stop-color="${darkColor}" />
        </linearGradient>
        <filter id="shadow" x="-20%" y="-20%" width="140%" height="140%">
          <feDropShadow dx="2" dy="3" stdDeviation="3" flood-color="#000000" flood-opacity="0.3"/>
        </filter>
      </defs>
      
      <!-- Background with rounded corners -->
      <rect width="100%" height="100%" fill="url(#bgGradient)" rx="12" ry="12" filter="url(#shadow)"/>
      
      <!-- Icon -->
      <text x="50%" y="40%" text-anchor="middle" dominant-baseline="middle"
            font-family="Arial, Helvetica, sans-serif" 
            font-size="${iconSize}" 
            fill="white" 
            filter="url(#shadow)">
        ${config.icon}
      </text>
      
      <!-- Title -->
      <text x="50%" y="65%" text-anchor="middle" dominant-baseline="middle"
            font-family="Arial, Helvetica, sans-serif" 
            font-size="${fontSize}" 
            fill="white" 
            font-weight="bold"
            filter="url(#shadow)">
        ${escapeXml(title)}
      </text>
      
      <!-- File type label -->
      <text x="50%" y="80%" text-anchor="middle" dominant-baseline="middle"
            font-family="Arial, Helvetica, sans-serif" 
            font-size="${fontSize * 0.8}" 
            fill="white" 
            opacity="0.9"
            filter="url(#shadow)">
        ${config.label}
      </text>
    </svg>
  `.trim();
}

// Special SVG for audio files with waveform
async function generateAudioWaveformPlaceholder(
  inputPath: string,
  outputPath: string,
  width: number,
  height: number,
  title: string
): Promise<void> {
  const truncatedTitle = truncateText(title, 20);
  
  const svgContent = `
    <svg width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id="gradient" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stop-color="#667eea" />
          <stop offset="100%" stop-color="#764ba2" />
        </linearGradient>
        <filter id="shadow" x="-20%" y="-20%" width="140%" height="140%">
          <feDropShadow dx="2" dy="3" stdDeviation="3" flood-color="#000000" flood-opacity="0.3"/>
        </filter>
      </defs>
      
      <rect width="100%" height="100%" fill="url(#gradient)" rx="12" ry="12" filter="url(#shadow)"/>
      
      <text x="50%" y="35%" text-anchor="middle" dominant-baseline="middle"
            font-family="Arial, Helvetica, sans-serif" 
            font-size="24" 
            fill="white" 
            font-weight="bold">
        🎵
      </text>
      
      <text x="50%" y="55%" text-anchor="middle" dominant-baseline="middle"
            font-family="Arial, Helvetica, sans-serif" 
            font-size="14" 
            fill="white" 
            font-weight="bold">
        ${escapeXml(truncatedTitle)}
      </text>
      
      <text x="50%" y="75%" text-anchor="middle" dominant-baseline="middle"
            font-family="Arial, Helvetica, sans-serif" 
            font-size="12" 
            fill="white">
        Audio File
      </text>
      
      <!-- Waveform visualization -->
      <path d="M${width * 0.1} ${height * 0.85} 
               L${width * 0.3} ${height * 0.4} 
               L${width * 0.5} ${height * 0.75} 
               L${width * 0.7} ${height * 0.5} 
               L${width * 0.9} ${height * 0.85}" 
            stroke="white" 
            stroke-width="2" 
            fill="none" 
            opacity="0.8"/>
    </svg>
  `.trim();
  
  writeFileSync(outputPath, svgContent);
}

// Keep all your existing helper functions
function getFileCategory(fileType: string): string {
  if (fileType.includes('pdf')) return 'pdf';
  if (fileType.includes('word') || fileType.includes('document')) return 'document';
  if (fileType.includes('powerpoint') || fileType.includes('presentation')) return 'presentation';
  if (fileType.includes('excel') || fileType.includes('spreadsheet')) return 'spreadsheet';
  if (fileType.includes('video')) return 'video';
  if (fileType.includes('audio')) return 'audio';
  if (fileType.includes('image')) return 'image';
  if (fileType.includes('zip') || fileType.includes('archive')) return 'archive';
  if (fileType.includes('text') || fileType.includes('code')) return 'code';
  return 'default';
}

function darkenColor(color: string, percent: number): string {
  const num = parseInt(color.replace("#", ""), 16);
  const amt = Math.round(2.55 * percent);
  const R = (num >> 16) - amt;
  const G = (num >> 8 & 0x00FF) - amt;
  const B = (num & 0x0000FF) - amt;
  return "#" + (
    0x1000000 +
    (R < 255 ? R < 1 ? 0 : R : 255) * 0x10000 +
    (G < 255 ? G < 1 ? 0 : G : 255) * 0x100 +
    (B < 255 ? B < 1 ? 0 : B : 255)
  ).toString(16).slice(1);
}

function escapeXml(unsafe: string): string {
  return unsafe.replace(/[<>&'"]/g, (c) => {
    switch (c) {
      case '<': return '&lt;';
      case '>': return '&gt;';
      case '&': return '&amp;';
      case '\'': return '&apos;';
      case '"': return '&quot;';
      default: return c;
    }
  });
}

function truncateText(text: string, maxLength: number): string {
  if (text.length <= maxLength) return text;
  return text.substring(0, maxLength) + '...';
}

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}