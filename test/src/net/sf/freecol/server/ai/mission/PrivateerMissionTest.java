 /**
 *  Copyright (C) 2002-2008  The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.server.ai.mission;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.control.Controller;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.MockMapGenerator;


public class PrivateerMissionTest extends FreeColTestCase {

    private static final GoodsType musketsType
        = spec().getGoodsType("model.goods.muskets");

    private static final TileType plains
        = spec().getTileType("model.tile.plains");

    private static final UnitType privateerType
        = spec().getUnitType("model.unit.privateer");
    private static final UnitType galleonType
        = spec().getUnitType("model.unit.galleon");


    @Override
    public void tearDown() throws Exception {
        ServerTestHelper.stopServerGame();
        super.tearDown();
    }


    private void setupPrivateerTestMap(Game game) {
        Map map = game.getMap();

        // Create player and unit
        ServerPlayer french = (ServerPlayer) game.getPlayer("model.nation.french");
        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");

        Tile colonyTile = map.getTile(9, 9);
        Tile privateerTile = map.getTile(10, 9);
        Tile frenchGalleonTile = map.getTile(11, 9);
        Tile dutchGalleonTile = map.getTile(12, 9);
        assertTrue("Setup error, colony tile should be land",colonyTile.isLand());
        assertFalse("Privateer tile should be ocean",privateerTile.isLand());
        assertFalse("French galleon tile should be ocean",frenchGalleonTile.isLand());
        assertFalse("Dutch galleon tile should be ocean",dutchGalleonTile.isLand());

        // setup colony and units
        getStandardColony(1, colonyTile.getX(), colonyTile.getY());
        new ServerUnit(game, privateerTile, dutch, privateerType,
                       UnitState.ACTIVE);
        new ServerUnit(game, frenchGalleonTile, french, galleonType,
                       UnitState.ACTIVE);
        new ServerUnit(game, dutchGalleonTile, dutch, galleonType,
                       UnitState.ACTIVE);
    }

    private Game setupPrivateerTestGame() {
        Map map = getCoastTestMap(plains);
        Game game = ServerTestHelper.startServerGame(map);
        setupPrivateerTestMap(game);
        return game;
    }

    /**
     * Tests validity of mission assignment
     */
    public void testIsMissionValid() {
        Game game = setupPrivateerTestGame();
        Map map = game.getMap();
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        Tile privateerTile = map.getTile(10, 9);
        Tile dutchGalleonTile = map.getTile(12, 9);

        Unit privateer = privateerTile.getFirstUnit();
        assertNotNull("Setup error, couldnt get privateer", privateer);
        Unit dutchGalleon = dutchGalleonTile.getFirstUnit();
        assertNotNull("Setup error, couldnt get galleon", dutchGalleon);

        AIPlayer aiPlayer = (AIPlayer)aiMain.getAIObject(privateer.getOwner().getId());
        AIUnit privateerAI = (AIUnit) aiMain.getAIObject(privateer);
        assertNotNull("Setup error, couldnt get privateerAI", privateerAI);
        // test PrivateerMission assignment
        String errMsg = "Privateer should not be allowed a PrivateerMission, no TransportMission assigned";
        assertFalse(errMsg,PrivateerMission.isValid(privateerAI));

        //Lets assign a transport mission to the privateer
        privateerAI.setMission(new TransportMission(aiMain,privateerAI));
        errMsg = "Privateer should have a TransportMission assigned";
        assertTrue(errMsg,privateerAI.getMission() instanceof TransportMission);
        int transportMissions = TransportMission.getPlayerNavalTransportMissionCount(aiPlayer,null);
        assertEquals("Wrong number of TransportMission assignments",1,transportMissions);
        // test PrivateerMission assignment
        errMsg = "Privateer should not be allowed a PrivateerMission, no other unit has a TransportMission assigned";
        assertFalse(errMsg,PrivateerMission.isValid(privateerAI));

        // Lets assign a transport mission to the galleon
        // We now will have more than one unit with a TransportMission
        AIUnit galleonAI = (AIUnit) aiMain.getAIObject(dutchGalleon);
        assertNotNull("Setup error, couldnt get galleonAI", galleonAI);
        galleonAI.setMission(new TransportMission(aiMain,galleonAI));
        errMsg = "Galleon should have a TransportMission assigned";
        assertTrue(errMsg,galleonAI.getMission() instanceof TransportMission);
        transportMissions = TransportMission.getPlayerNavalTransportMissionCount(aiPlayer,null);
        assertEquals("Wrong number of TransportMission assignments",2,transportMissions);
        // test PrivateerMission assignment
        errMsg = "Privateer should be allowed a PrivateerMission, another unit has a TransportMission assigned";
        assertTrue(errMsg,PrivateerMission.isValid(privateerAI));
    }
}
