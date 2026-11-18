package com.coldfriend.interaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.coldfriend.entity.EntityId;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InteractableRefTest {

    @Test
    void refsToTheSameThingAreEqualWhereverTheyAreMade() {
        InteractableRef a = InteractableRef.item(new EntityId(7L));
        InteractableRef b = InteractableRef.item(new EntityId(7L));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        Set<InteractableRef> set = new HashSet<>();
        set.add(a);
        set.add(b);
        assertEquals(1, set.size());
    }

    @Test
    void differentKindsOrNumbersAreDifferent() {
        assertNotEquals(InteractableRef.item(new EntityId(1L)), InteractableRef.CABIN_DOOR);
        assertNotEquals(InteractableRef.CABIN_DOOR, InteractableRef.CABIN_HEARTH);
        assertNotEquals(InteractableRef.item(new EntityId(1L)), InteractableRef.item(new EntityId(2L)));
    }

    @Test
    void anItemRefGivesBackItsEntityId() {
        assertEquals(new EntityId(42L), InteractableRef.item(new EntityId(42L)).entity());
        assertThrows(IllegalStateException.class, InteractableRef.CABIN_DOOR::entity);
    }

    @Test
    void aKindIsRequired() {
        assertThrows(IllegalArgumentException.class, () -> new InteractableRef(null, 1L));
    }
}
