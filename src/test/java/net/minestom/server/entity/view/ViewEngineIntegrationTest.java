package net.minestom.server.entity.view;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@EnvTest
public class ViewEngineIntegrationTest {

    @Test
    public void noViewersForEntityTest(Env env) {
        final Instance instance = env.createFlatInstance();
        final Entity entity = new Entity(EntityType.ZOMBIE);
        entity.setInstance(instance, new Pos(0, 42, 0));

        assertEquals(0, entity.getViewers().size());
    }

    @Test
    public void noViewersForPlayerTest(Env env) {
        final Instance instance = env.createFlatInstance();
        final Player player = env.createPlayer(instance, new Pos(0, 42, 0));

        assertEquals(0, player.getViewers().size());
    }

    @Test
    public void removingAndAddingPlayersTest(Env env) {
        final Instance instance = env.createFlatInstance();

        final Player player1 = env.createPlayer(instance, new Pos(0, 42, 0));
        final Player player2 = env.createPlayer(instance, new Pos(0, 42, 0));

        assertEquals(1, player1.getViewers().size());
        assertTrue(player1.getViewers().contains(player2));

        assertEquals(1, player2.getViewers().size());
        assertTrue(player2.getViewers().contains(player1));

        player2.remove();

        assertEquals(0, player1.getViewers().size());
        assertEquals(0, player2.getViewers().size());

        final Player player3 = env.createPlayer(instance, new Pos(0, 42, 0));

        assertEquals(1, player1.getViewers().size());
        assertTrue(player1.getViewers().contains(player3));

        assertEquals(1, player3.getViewers().size());
        assertTrue(player3.getViewers().contains(player1));
    }

    @Test
    public void longRangeManualViewersTest(Env env) {
        final Instance instance = env.createFlatInstance();

        final Player player1 = env.createPlayer(instance, new Pos(0, 42, 0));
        final Player player2 = env.createPlayer(instance, new Pos(0, 42, 5000));

        assertEquals(0, player1.getViewers().size());
        assertEquals(0, player2.getViewers().size());

        player1.addViewer(player2);

        assertEquals(1, player1.getViewers().size());
        assertTrue(player1.getViewers().contains(player2));

        assertEquals(0, player2.getViewers().size());

        player2.teleport(new Pos(0, 42, 0)).join();

        assertEquals(1, player1.getViewers().size());
        assertEquals(1, player2.getViewers().size());
    }

    @Test
    public void shortRangeViewersTest(Env env) {
        final Instance instance = env.createFlatInstance();

        final Player player1 = env.createPlayer(instance, new Pos(0, 42, 0));
        final Player player2 = env.createPlayer(instance, new Pos(0, 42, 96));

        assertEquals(0, player1.getViewers().size());
        assertEquals(0, player2.getViewers().size());

        player2.teleport(new Pos(0, 42, 95)).join(); // teleport in range (6 chunks)

        assertEquals(1, player1.getViewers().size());
        assertEquals(1, player2.getViewers().size());
    }

    @Test
    public void autoViewableTest(Env env) {
        final Instance instance = env.createFlatInstance();

        final Player player1 = env.createPlayer(instance, new Pos(0, 42, 0));
        final Player player2 = env.createPlayer(instance, new Pos(0, 42, 0));

        final Entity entity = new Entity(EntityType.ZOMBIE);
        entity.setInstance(instance, new Pos(0, 42, 0)).join();

        assertEquals(1, player1.getViewers().size());
        assertEquals(1, player2.getViewers().size());
        assertEquals(2, entity.getViewers().size());

        assertTrue(player1.isAutoViewable());
        assertTrue(player2.isAutoViewable());
        assertTrue(entity.isAutoViewable());

        player1.hide();

        assertEquals(0, player1.getViewers().size());
        assertEquals(1, player2.getViewers().size());
        assertEquals(2, entity.getViewers().size());

        player1.show();

        assertEquals(1, player1.getViewers().size());
        assertEquals(1, player2.getViewers().size());
        assertEquals(2, entity.getViewers().size());

        entity.hide();

        assertEquals(0, entity.getViewers().size());

        entity.show();

        assertEquals(2, entity.getViewers().size());
    }

    @Test
    public void setViewableRuleTest(Env env) {
        final Instance instance = env.createFlatInstance();

        final Player player1 = env.createPlayer(instance, new Pos(0, 42, 0));
        final Player player2 = env.createPlayer(instance, new Pos(0, 42, 0));

        final Entity entity = new Entity(EntityType.ZOMBIE);
        entity.setInstance(instance, new Pos(0, 42, 0)).join();

        assertEquals(1, player1.getViewers().size());
        assertEquals(1, player2.getViewers().size());
        assertEquals(2, entity.getViewers().size());

        // only player1 should be able to see this entity
        entity.setViewableRule(player -> player == player1);

        assertEquals(1, entity.getViewers().size());
        assertTrue(entity.hasViewer(player1));
        assertFalse(entity.hasViewer(player2));

        // only player2 should be able to see this entity
        entity.setViewableRule(player -> player == player2);

        assertEquals(1, entity.getViewers().size());
        assertTrue(entity.hasViewer(player2));
        assertFalse(entity.hasViewer(player1));
    }

    @Test
    public void autoViewEntitiesTest(Env env) {
        final Instance instance = env.createFlatInstance();

        final Player player1 = env.createPlayer(instance, new Pos(0, 42, 0));
        player1.hideEntities();

        final Player player2 = env.createPlayer(instance, new Pos(0, 42, 0));

        final Entity entity = new Entity(EntityType.ZOMBIE);
        entity.setInstance(instance, new Pos(0, 42, 0)).join();

        assertEquals(1, player1.getViewers().size());
        assertEquals(0, player2.getViewers().size());
        assertEquals(1, entity.getViewers().size());

        player2.hideEntities();

        assertEquals(0, player1.getViewers().size());
        assertEquals(0, player2.getViewers().size());
        assertEquals(0, entity.getViewers().size());

        player1.showEntities();
        player2.showEntities();

        assertEquals(1, player1.getViewers().size());
        assertEquals(1, player2.getViewers().size());
        assertEquals(2, entity.getViewers().size());
    }

    @Test
    public void setViewerRuleTest(Env env) {
        final Instance instance = env.createFlatInstance();

        final Player player1 = env.createPlayer(instance, new Pos(0, 42, 0));
        final Player player2 = env.createPlayer(instance, new Pos(0, 42, 0));

        final Entity entity = new Entity(EntityType.ZOMBIE);
        entity.setInstance(instance, new Pos(0, 42, 0)).join();

        // player1 can only see player2
        player1.setViewerRule(entity1 -> entity1 == player2);

        assertEquals(1, player1.getViewers().size());
        assertEquals(1, player2.getViewers().size());
        assertEquals(1, entity.getViewers().size());

        // player1 can only see the entity
        // player2 can only see the entity
        player1.setViewerRule(entity1 -> entity1 == entity);
        player2.setViewerRule(entity1 -> entity1 == entity);

        assertEquals(0, player1.getViewers().size());
        assertEquals(0, player2.getViewers().size());
        assertEquals(2, entity.getViewers().size());
    }

    @Test
    public void predictableViewersTest(Env env) {
        final Instance instance = env.createFlatInstance();

        final Player player = env.createPlayer(instance, new Pos(0, 42, 0));

        assertTrue(player.hasPredictableViewers());

        player.hide();

        assertFalse(player.hasPredictableViewers());

        player.show();

        assertTrue(player.hasPredictableViewers());

        final Player tempPlayer = env.createPlayer(instance, new Pos(0, 42, 0));
        player.addViewer(tempPlayer);

        assertFalse(player.hasPredictableViewers());

        player.removeViewer(tempPlayer);
        tempPlayer.remove();

        assertTrue(player.hasPredictableViewers());

        player.setViewableRule(_ -> false);

        assertFalse(player.hasPredictableViewers());

        player.show();

        assertTrue(player.hasPredictableViewers());
    }
}