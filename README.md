# Luminor

Proof of concept Fabric mod that replaces the block light attenuation model with a spherical voxel lighting method.

## How does it work?

In the typical vanilla lighting model, light sources propagate light in the six axis-aligned directions of north, south, east, west, up, and down, and for each successive step the light level
is decremented by 1 (the attenuation amount). This results in the familiar diamond pattern.

The method employed by Luminor approximates spherical light attenuation by additionally propagating light into the diagonally adjacent blocks to a light source. On each step into the diagonals, light is attenuated by 1.5 instead of 1, and on each step into the corner blocks the attentuation is 1.75. 

However, this method requires fractional light levels. To technically achieve this, the lighting system in Minecraft is augmented by dividing each of the 16 light levels into 8 sub-levels, for a total of 128 internal light levels. This allows Luminor to attenuate light by 1.5 or 1.75 levels by instead attenuating by 12 or 14 sub-levels, respectively.

The internal light levels are exposed to the rest of the Minecraft engine by integral division of the internal light level by 8 -- thus returning the scale back to 0 to 15, like in vanilla.

This ensures that existing vanilla and modded mechanics which rely on the vanilla lighting behavior will interact properly with the spherical light volumes.

This mod is Fabric-only. It replaces many fundamental parts of the lighting code, so Forge is out of the question.