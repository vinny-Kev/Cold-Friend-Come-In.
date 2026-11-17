package com.coldfriend.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MenuModelTest {

    private static MenuModel menu(List<Integer> confirmations, int[] adjustments) {
        return new MenuModel(List.of(
                MenuItem.action("New Game", () -> confirmations.add(0)),
                MenuItem.disabled("Continue"),
                MenuItem.action("Settings", () -> confirmations.add(2)),
                MenuItem.value("Volume", delta -> adjustments[0] += delta),
                MenuItem.action("Quit", () -> confirmations.add(4))));
    }

    @Test
    void initialSelectionIsTheFirstEnabledItem() {
        MenuModel model = menu(new ArrayList<>(), new int[1]);

        assertEquals(0, model.selectedIndex());
    }

    @Test
    void navigationSkipsDisabledItems() {
        MenuModel model = menu(new ArrayList<>(), new int[1]);

        model.moveSelection(1);

        assertEquals(2, model.selectedIndex());
    }

    @Test
    void navigationWrapsAroundInBothDirections() {
        MenuModel model = menu(new ArrayList<>(), new int[1]);

        model.moveSelection(-1);
        assertEquals(4, model.selectedIndex());

        model.moveSelection(1);
        assertEquals(0, model.selectedIndex());
    }

    @Test
    void disabledItemCannotBeSelected() {
        List<Integer> confirmations = new ArrayList<>();
        MenuModel model = menu(confirmations, new int[1]);

        model.select(1);
        model.confirmSelected();

        assertEquals(0, model.selectedIndex());
        assertEquals(List.of(0), confirmations);
    }

    @Test
    void clickingDisabledItemRunsNothing() {
        List<Integer> confirmations = new ArrayList<>();
        MenuModel model = menu(confirmations, new int[1]);

        model.activate(1);

        assertEquals(0, model.selectedIndex());
        assertEquals(List.of(), confirmations);
    }

    @Test
    void confirmRunsTheSelectedAction() {
        List<Integer> confirmations = new ArrayList<>();
        MenuModel model = menu(confirmations, new int[1]);

        model.moveSelection(1);
        model.confirmSelected();

        assertEquals(List.of(2), confirmations);
    }

    @Test
    void adjustChangesValueAndConfirmAddsOneStep() {
        int[] value = new int[1];
        MenuModel model = menu(new ArrayList<>(), value);
        model.select(3);

        model.adjustSelected(-1);
        model.confirmSelected();

        assertEquals(0, value[0]);
    }
}
