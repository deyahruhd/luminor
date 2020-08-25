package hu.jard.luminor;

import net.fabricmc.api.ModInitializer;

@SuppressWarnings ("unused")
public class Luminor implements ModInitializer {
	public static final int LIGHT_SUBDIVS = 8;
	public static final int LIGHT_LEVEL_COUNT = LIGHT_SUBDIVS * 16;
	public static final int MAX_LIGHT_LEVEL = LIGHT_LEVEL_COUNT - 1;


	public static final int FACE_ATTENUATION = LIGHT_SUBDIVS;
	public static final int EDGE_ATTENUATION = (int) (LIGHT_SUBDIVS * 1.5);
	public static final int CRNR_ATTENUATION = (int) (LIGHT_SUBDIVS * 1.75);

	public enum Direction {
		POS_Z ( 0,  0,  1, FACE_ATTENUATION),
		NEG_Z ( 0,  0, -1, FACE_ATTENUATION),
		POS_Y ( 0,  1,  0, FACE_ATTENUATION),
		NEG_Y ( 0, -1,  0, FACE_ATTENUATION),
		POS_X ( 1,  0,  0, FACE_ATTENUATION),
		NEG_X (-1,  0,  0, FACE_ATTENUATION),

		POS_X_POS_Z ( 1,  0,  1, EDGE_ATTENUATION),
		NEG_X_POS_Z (-1,  0,  1, EDGE_ATTENUATION),
		POS_Y_POS_Z ( 0,  1,  1, EDGE_ATTENUATION),
		NEG_Y_POS_Z ( 0, -1,  1, EDGE_ATTENUATION),
		POS_X_NEG_Z ( 1,  0, -1, EDGE_ATTENUATION),
		NEG_X_NEG_Z (-1,  0, -1, EDGE_ATTENUATION),
		POS_Y_NEG_Z ( 0,  1, -1, EDGE_ATTENUATION),
		NEG_Y_NEG_Z ( 0, -1, -1, EDGE_ATTENUATION),
		POS_X_POS_Y ( 1,  1,  0, EDGE_ATTENUATION),
		NEG_X_POS_Y (-1,  1,  0, EDGE_ATTENUATION),
		POS_X_NEG_Y ( 1, -1,  0, EDGE_ATTENUATION),
		NEG_X_NEG_Y (-1, -1,  0, EDGE_ATTENUATION),

		POS_X_POS_Y_POS_Z ( 1,  1,  1, CRNR_ATTENUATION),
		NEG_X_POS_Y_POS_Z (-1,  1,  1, CRNR_ATTENUATION),
		POS_X_NEG_Y_POS_Z ( 1, -1,  1, CRNR_ATTENUATION),
		NEG_X_NEG_Y_POS_Z (-1, -1,  1, CRNR_ATTENUATION),
		POS_X_POS_Y_NEG_Z ( 1,  1, -1, CRNR_ATTENUATION),
		NEG_X_POS_Y_NEG_Z (-1,  1, -1, CRNR_ATTENUATION),
		POS_X_NEG_Y_NEG_Z ( 1, -1, -1, CRNR_ATTENUATION),
		NEG_X_NEG_Y_NEG_Z (-1, -1, -1, CRNR_ATTENUATION);

		Direction (int dirX, int dirY, int dirZ, int dirAtten) {
			this.x = dirX;
			this.y = dirY;
			this.z = dirZ;

			this.attenuation = dirAtten;
		}

		public static Direction from (int x, int y, int z) {
			boolean posX = x > 0;
			boolean negX = x < 0;
			boolean zroX = !posX && !negX;

			boolean posY = y > 0;
			boolean negY = y < 0;
			boolean zroY = !posY && !negY;

			boolean posZ = z > 0;
			boolean negZ = z < 0;
			boolean zroZ = !posZ && !negZ;

			if (posX && zroY && zroZ)
				return POS_X;
			if (negX && zroY && zroZ)
				return NEG_X;
			if (zroX && posY && zroZ)
				return POS_Y;
			if (zroX && negY && zroZ)
				return NEG_Y;
			if (zroX && zroY && posZ)
				return POS_Z;
			if (zroX && zroY && negZ)
				return NEG_Z;

			if (posX && posY && zroZ)
				return POS_X_POS_Y;
			if (negX && posY && zroZ)
				return NEG_X_POS_Y;
			if (posX && negY && zroZ)
				return POS_X_NEG_Y;
			if (negX && negY && zroZ)
				return NEG_X_NEG_Y;
			if (posX && zroY && posZ)
				return POS_X_POS_Z;
			if (negX && zroY && posZ)
				return NEG_X_POS_Z;
			if (posX && zroY && negZ)
				return POS_X_NEG_Z;
			if (negX && zroY && negZ)
				return NEG_X_NEG_Z;

			if (zroX && posY && posZ)
				return POS_X_POS_Z;
			if (zroX && negY && posZ)
				return NEG_Y_POS_Z;
			if (zroX && posY && negZ)
				return POS_Y_NEG_Z;
			if (zroX && negY && negZ)
				return NEG_Y_NEG_Z;

			if (posX && posY && posZ)
				return POS_X_POS_Y_POS_Z;
			if (negX && posY && posZ)
				return NEG_X_POS_Y_POS_Z;
			if (posX && negY && posZ)
				return POS_X_NEG_Y_POS_Z;
			if (negX && negY && posZ)
				return NEG_X_NEG_Y_POS_Z;
			if (posX && posY && negZ)
				return POS_X_POS_Y_NEG_Z;
			if (negX && posY && negZ)
				return NEG_X_POS_Y_NEG_Z;
			if (posX && negY && negZ)
				return POS_X_NEG_Y_NEG_Z;
			if (negX && negY && negZ)
				return NEG_X_NEG_Y_NEG_Z;

			return null;
		}

		public final int x, y, z, attenuation;
	}

	public static final Luminor.Direction[] HORIZONTAL_DIRECTIONS = new Luminor.Direction[] {
			Luminor.Direction.NEG_X,
			Luminor.Direction.POS_X,
			Luminor.Direction.NEG_X,
			Luminor.Direction.POS_Z,
			Luminor.Direction.NEG_X_NEG_Y,
			Luminor.Direction.POS_X_NEG_Y,
			Luminor.Direction.NEG_Y_NEG_Z,
			Luminor.Direction.NEG_Y_POS_Z,

			Luminor.Direction.NEG_X_NEG_Y_NEG_Z,
			Luminor.Direction.POS_X_NEG_Y_NEG_Z,
			Luminor.Direction.NEG_X_NEG_Y_POS_Z,
			Luminor.Direction.POS_X_NEG_Y_POS_Z };

	@Override
	public void onInitialize () { }
}
