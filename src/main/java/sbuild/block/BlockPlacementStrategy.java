package sbuild.block;

import net.minecraft.util.math.Direction;

/**
 * Strategy contract for placing complex blocks with orientation-sensitive logic.
 */
public interface BlockPlacementStrategy {
    boolean supports(PlacementContext context);

    PlacementContext determinePlacementContext(PlacementContext rawContext);

    Direction determinePlayerLookDirection(PlacementContext context);

    Direction determineFaceToClick(PlacementContext context, Direction lookDirection);

    PlacementPlan createPlan(PlacementContext context, Direction lookDirection, Direction faceToClick);

    boolean performPlacement(PlacementContext context, PlacementPlan plan);

    boolean validateResult(PlacementContext context, PlacementPlan plan);

    default boolean place(PlacementContext rawContext) {
        PlacementContext context = determinePlacementContext(rawContext);
        Direction lookDirection = determinePlayerLookDirection(context);
        Direction faceToClick = determineFaceToClick(context, lookDirection);
        PlacementPlan plan = createPlan(context, lookDirection, faceToClick);
        if (!performPlacement(context, plan)) {
            return false;
        }
        return validateResult(context, plan);
    }
}
