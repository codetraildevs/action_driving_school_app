package com.drivingschoolrwandaapp.utils;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import org.junit.Test;
import org.mockito.InOrder;
import static org.mockito.Mockito.*;

public class LocaleUiTest {
    @Test public void refreshRetainsFragmentAndRestoresViewStateWithoutChangingBackStack() {
        Fragment fragment = mock(Fragment.class);
        FragmentManager manager = mock(FragmentManager.class);
        FragmentTransaction detach = mock(FragmentTransaction.class, RETURNS_SELF);
        FragmentTransaction attach = mock(FragmentTransaction.class, RETURNS_SELF);
        View view = mock(View.class);
        when(fragment.isAdded()).thenReturn(true);
        when(fragment.getView()).thenReturn(view);
        when(fragment.requireView()).thenReturn(view);
        when(fragment.getParentFragmentManager()).thenReturn(manager);
        when(manager.beginTransaction()).thenReturn(detach, attach);

        LocaleUi.refreshFragmentView(fragment);

        InOrder order = inOrder(view, detach, attach);
        order.verify(view).saveHierarchyState(any());
        order.verify(detach).detach(fragment);
        order.verify(detach).commitNow();
        order.verify(attach).attach(fragment);
        order.verify(attach).commitNow();
        order.verify(view).restoreHierarchyState(any());
        verify(detach, never()).addToBackStack(any());
        verify(attach, never()).addToBackStack(any());
    }

    @Test public void refreshDoesNotCommitAfterStateSaved() {
        Fragment fragment = mock(Fragment.class);
        FragmentManager manager = mock(FragmentManager.class);
        when(fragment.isAdded()).thenReturn(true);
        when(fragment.getView()).thenReturn(mock(View.class));
        when(fragment.getParentFragmentManager()).thenReturn(manager);
        when(manager.isStateSaved()).thenReturn(true);
        LocaleUi.refreshFragmentView(fragment);
        verify(manager, never()).beginTransaction();
    }

    @Test public void refreshIgnoresDetachedFragment() {
        Fragment fragment = mock(Fragment.class);
        LocaleUi.refreshFragmentView(fragment);
        verify(fragment, never()).getParentFragmentManager();
    }

    @Test public void menuTranslationKeepsExistingItemsAndClickListeners() {
        Menu menu = mock(Menu.class);
        Menu translated = mock(Menu.class);
        MenuItem original = mock(MenuItem.class);
        MenuItem label = mock(MenuItem.class);
        when(translated.size()).thenReturn(1);
        when(translated.getItem(0)).thenReturn(label);
        when(label.getItemId()).thenReturn(42);
        when(label.getTitle()).thenReturn("Profil");
        when(menu.findItem(42)).thenReturn(original);
        LocaleUi.copyTitles(menu, translated);
        verify(original).setTitle("Profil");
        verify(menu, never()).clear();
        verify(original, never()).setOnMenuItemClickListener(any());
    }
}
