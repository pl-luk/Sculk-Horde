package com.github.sculkhoard.common.block;

import com.github.sculkhoard.common.tileentity.InfectedDirtTile;
import com.github.sculkhoard.core.BlockRegistry;
import com.github.sculkhoard.core.TileEntityRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.common.extensions.IForgeBlock;

import javax.annotation.Nullable;
import java.util.Random;

import static com.github.sculkhoard.core.SculkHoard.DEBUG_MODE;

public class InfectedDirtBlock extends Block implements IForgeBlock {

    /**
     * MATERIAL is simply what the block is made up. This affects its behavior & interactions.<br>
     * MAP_COLOR is the color that will show up on a map to represent this block
     */
    public static Material MATERIAL = Material.DIRT;
    public static MaterialColor MAP_COLOR = MaterialColor.COLOR_BROWN;

    /**
     * HARDNESS determines how difficult a block is to break<br>
     * 0.6f = dirt<br>
     * 1.5f = stone<br>
     * 2f = log<br>
     * 3f = iron ore<br>
     * 50f = obsidian
     */
    public static float HARDNESS = 0.6f;

    /**
     * BLAST_RESISTANCE determines how difficult a block is to blow up<br>
     * 0.5f = dirt<br>
     * 2f = wood<br>
     * 6f = cobblestone<br>
     * 1,200f = obsidian
     */
    public static float BLAST_RESISTANCE = 0.5f;

    /**
     * PREFERRED_TOOL determines what type of tool will break the block the fastest and be able to drop the block if possible
     */
    public static ToolType PREFERRED_TOOL = ToolType.SHOVEL;

    /**
     *  Harvest Level Affects what level of tool can mine this block and have the item drop<br>
     *
     *  -1 = All<br>
     *  0 = Wood<br>
     *  1 = Stone<br>
     *  2 = Iron<br>
     *  3 = Diamond<br>
     *  4 = Netherite
     */
    public static int HARVEST_LEVEL = -1;

    /**
     *  validSpreadBlocks is a list of blocks that infected dirt can spread to.<br>
     *  DEFAULT_MAX_SPREAD_ATTEMPTS is the max number of spread attempts assigned
     *  to this block by default.<br>
     *  This only really applies to the root block because every child of the root
     *  block will have a smaller amount of spread attempts assigned to it by the
     *  immediate parent.
     */
    public static Block[] validSpreadBlocks = {Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.GRASS_PATH};
    public static int DEFAULT_MAX_SPREAD_ATTEMPTS = 30;

    /**
     * The Constructor that takes in properties
     * @param prop The Properties
     */
    public InfectedDirtBlock(Properties prop) {
        super(prop);
    }

    /**
     * A simpler constructor that does not take in properties.<br>
     * I made this so that registering blocks in BlockRegistry.java can look cleaner
     */
    public InfectedDirtBlock() {
        this(getProperties());
    }

    /**
     * This function is called when this block is placed. <br>
     * Will set the nbt data maxSpreadAttempts.
     * @param world The world the block is in
     * @param bp The position the block is in
     * @param blockState The state of the block
     * @param entity The entity that placed it
     * @param itemStack The item stack it was placed from
     */
    @Override
    public void setPlacedBy(World world, BlockPos bp, BlockState blockState, @Nullable LivingEntity entity, ItemStack itemStack) {
        super.setPlacedBy(world, bp, blockState, entity, itemStack);
    }

    /**
     * Determines if this block will randomly tick or not.
     * @param blockState The current blockstate
     * @return True/False
     */
    @Override
    public boolean isRandomlyTicking(BlockState blockState) {
        return true;
    }

    /**
     * Gets called every time the block randomly ticks.
     * @param blockState The current Blockstate
     * @param serverWorld The current ServerWorld
     * @param bp The current Block Position
     * @param random ???
     */
    @Override
    public void randomTick(BlockState blockState, ServerWorld serverWorld, BlockPos bp, Random random) {

        //Get tile entity for this block
        TileEntity tileEntity = serverWorld.getBlockEntity(bp);
        InfectedDirtTile thisTile = null;

        //If there is no error with the tile entity, then increment spreadAttempts and spread to it.
        if(tileEntity instanceof InfectedDirtTile && tileEntity != null)
        {
            thisTile = ((InfectedDirtTile) tileEntity); //Cast
        }
        else
        {
            System.out.println("ERROR: InfectedDirtTile not found.");
        }

        //If this block has not attempted to spread before
        if(thisTile != null && thisTile.getMaxSpreadAttempts() == -1)
        {
            thisTile.setMaxSpreadAttempts(DEFAULT_MAX_SPREAD_ATTEMPTS);//Set to default
            if(DEBUG_MODE)
            {
                System.out.println("Block at (" +
                        bp.getX() + ", " +
                        bp.getY() + ", " +
                        bp.getZ() + ") " +
                        "is getting the DEFAULT_MAX_SPREAD_ATTEMPTS"
                );
            }
        }

        //If max spread attempts has not been reached && Given a 25% chance && the area is loaded, spread
        if(thisTile.getMaxSpreadAttempts() - thisTile.getSpreadAttempts() > 0 && serverWorld.random.nextInt(4) == 0 && serverWorld.isAreaLoaded(bp,4))
        {
            BlockPos spreadPosition = getRandomAdjacentBlockPos(bp, serverWorld); //Get a random adjacent block position
            Block spreadBlock = serverWorld.getBlockState(spreadPosition).getBlock(); //Get the block at this position
            attemptSpread(thisTile, serverWorld, spreadPosition, spreadBlock); //Attempt to spread to this position
        }
        //If this block has run out of spread attempts, convert to crust
        else if(thisTile.getMaxSpreadAttempts() - thisTile.getSpreadAttempts() <= 0)
        {
            serverWorld.setBlockAndUpdate(bp, BlockRegistry.CRUST.get().defaultBlockState());//Convert to crust
        }
    }

    /**
     * Attempts to spread infjected dirt to a specific block position.
     * @param thisTile The Tile Entity of this block
     * @param serverWorld The ServerWorld of this block
     * @param targetPos The Block Position of the target block
     * @param targetBlock The class of the target block
     */
    public void attemptSpread(InfectedDirtTile thisTile, ServerWorld serverWorld, BlockPos targetPos, Block targetBlock)
    {
        thisTile.setSpreadAttempts(thisTile.getSpreadAttempts() + 1); //Increment spreadAttempts
        //If target block is one that we can spread to, spread to it.
        if(isValidSpreadBlock(targetBlock))
        {
            if(DEBUG_MODE)
            {
                System.out.println("New Infected Dirt at (" +
                        targetPos.getX() + ", " +
                        targetPos.getY() + ", " +
                        targetPos.getZ() + ") "
                );
            }
            serverWorld.setBlockAndUpdate(targetPos, this.defaultBlockState()); //Set the block
            TileEntity childTile = serverWorld.getWorldServer().getBlockEntity(targetPos); //Get new block tile entity

            //If no error with tile entity of child block
            if(childTile instanceof InfectedDirtTile && childTile != null)
            {
                //De-increment maxSpreadAttempts of child
                ((InfectedDirtTile) childTile).setMaxSpreadAttempts(thisTile.getMaxSpreadAttempts() - 1);
            }
            else
            {
                System.out.println("ERROR: Child InfectedDirtTile not found.");
            }
        }
    }

    /**
     * Choose random adjacent position to spread to
     * @param origin The origin Block Position
     * @param serverWorld The ServerWorld of the block
     * @return The target Block Position
     */
    public BlockPos getRandomAdjacentBlockPos(BlockPos origin, ServerWorld serverWorld)
    {
        BlockPos UP = new BlockPos(origin.getX(), origin.getY() + 1, origin.getZ());
        BlockPos DOWN = new BlockPos(origin.getX(), origin.getY() - 1, origin.getZ());
        BlockPos FRONT = new BlockPos(origin.getX() + 1, origin.getY(), origin.getZ());
        BlockPos BACK = new BlockPos(origin.getX() - 1, origin.getY(), origin.getZ());
        BlockPos RIGHT = new BlockPos(origin.getX(), origin.getY(), origin.getZ() + 1);
        BlockPos LEFT = new BlockPos(origin.getX() - 1, origin.getY(), origin.getZ());
        BlockPos[] spreadDirections = {UP, DOWN, FRONT, BACK, RIGHT, LEFT};
        return spreadDirections[serverWorld.random.nextInt(6)];
    }

    /**
     * Linear search through validSpreadBlocks array to check if
     * the given block is in that list.
     * @param block The block in question
     * @return True/False
     */
    public boolean isValidSpreadBlock(Block block)
    {
        for(Block b : validSpreadBlocks)
        {
            if(b.is(block))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the state that this block should transform into when right clicked by a tool.
     * For example: Used to determine if an axe can strip, a shovel can path, or a hoe can till.
     * Return null if vanilla behavior should be disabled.
     *
     * @param state The current state
     * @param world The world
     * @param pos The block position in world
     * @param player The player clicking the block
     * @param stack The stack being used by the player
     * @return The resulting state after the action has been performed
     */
    public BlockState getToolModifiedState(BlockState state, World world, BlockPos pos, PlayerEntity player, ItemStack stack, ToolType toolType)
    {
        if(DEBUG_MODE)
        {
            TileEntity tile = world.getBlockEntity(pos);
            if(tile instanceof InfectedDirtTile && tile != null)
            {

                System.out.println("Block at (" +
                        pos.getX() + ", " +
                        pos.getY() + ", " +
                        pos.getZ() + ") " +
                        "maxSpreadAttempts: " + ((InfectedDirtTile) tile).getMaxSpreadAttempts() +
                        " spreadAttempts: " + ((InfectedDirtTile) tile).getSpreadAttempts()
                );
            }
            else
            {
                System.out.println("Error accessing InfectedDirtTile");
            }
        }

        return null; //Just Return null because We Are Not Modifying it
    }

    /**
     * Determines if a specified mob type can spawn on this block, returning false will
     * prevent any mob from spawning on the block.
     *
     * @param state The current state
     * @param world The current world
     * @param pos Block position in world
     * @param type The Mob Category Type
     * @return True to allow a mob of the specified category to spawn, false to prevent it.
     */
    public boolean canCreatureSpawn(BlockState state, IBlockReader world, BlockPos pos, EntitySpawnPlacementRegistry.PlacementType type, EntityType<?> entityType)
    {
        return false;
    }

    /**
     * Determines the properties of a block.<br>
     * I made this in order to be able to establish a block's properties from within the block class and not in the BlockRegistry.java
     * @return The Properties of the block
     */
    public static Properties getProperties()
    {
        Properties prop = Properties.of(MATERIAL, MAP_COLOR)
                .strength(HARDNESS, BLAST_RESISTANCE)
                .harvestTool(PREFERRED_TOOL)
                .harvestLevel(HARVEST_LEVEL)
                .sound(SoundType.GRASS);
        return prop;
    }

    /**
     * A function called by forge to create the tile entity.
     * @param state The current blockstate
     * @param world The world the block is in
     * @return Returns the tile entity.
     */
    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return TileEntityRegistry.INFECTED_DIRT_TILE.get().create();
    }

    /**
     * Returns If true we have a tile entity
     * @param state The current block state
     * @return True
     */
    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

}