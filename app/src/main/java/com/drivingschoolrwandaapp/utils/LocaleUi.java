package com.drivingschoolrwandaapp.utils;

import android.content.Context;
import android.os.Parcelable;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavDestination;
import androidx.navigation.NavGraph;

/** Refresh translated content without destroying the Activity's secure window. */
public final class LocaleUi {
    private LocaleUi() { }

    public static void refreshFragmentView(Fragment fragment) {
        if (fragment == null || !fragment.isAdded() || fragment.getView() == null) return;
        FragmentManager manager = fragment.getParentFragmentManager();
        if (manager.isStateSaved()) return;
        SparseArray<Parcelable> state = new SparseArray<>();
        fragment.requireView().saveHierarchyState(state);
        // Separate transactions are intentional: a combined detach/attach is optimized away.
        // The same Fragment, ViewModels and navigation back stack are retained.
        manager.beginTransaction().detach(fragment).commitNow();
        manager.beginTransaction().attach(fragment).commitNow();
        if (fragment.getView() != null) fragment.requireView().restoreHierarchyState(state);
    }

    public static void refreshMenu(Context context, View anchor, Menu menu, int resource) {
        PopupMenu translated = new PopupMenu(context, anchor);
        translated.inflate(resource);
        copyTitles(menu, translated.getMenu());
    }

    static void copyTitles(Menu target, Menu translated) {
        for (int i = 0; i < translated.size(); i++) {
            MenuItem source = translated.getItem(i);
            MenuItem item = target.findItem(source.getItemId());
            if (item == null) continue;
            item.setTitle(source.getTitle());
            if (item.hasSubMenu() && source.hasSubMenu()) {
                copyTitles(item.getSubMenu(), source.getSubMenu());
            }
        }
    }

    public static void refreshLabels(NavGraph target, NavGraph translated) {
        for (NavDestination source : translated) {
            NavDestination destination = target.findNode(source.getId());
            if (destination == null) continue;
            destination.setLabel(source.getLabel());
            if (destination instanceof NavGraph && source instanceof NavGraph) {
                refreshLabels((NavGraph) destination, (NavGraph) source);
            }
        }
    }
}
