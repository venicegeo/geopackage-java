package mil.nga.geopackage.tiles;

import mil.nga.geopackage.tiles.user.TileDao;
import mil.nga.sf.GeometryEnvelope;
import mil.nga.sf.projection.Projection;
import mil.nga.sf.projection.ProjectionConstants;
import mil.nga.sf.projection.ProjectionFactory;

/**
 * GeoPackage Tile Retriever, retrieves a tile from a GeoPackage from XYZ
 * coordinates
 * 
 * @author osbornb
 * @since 1.2.0
 */
public class GeoPackageTileRetriever implements TileRetriever {

	/**
	 * Tile Creator
	 */
	private final TileCreator tileCreator;

	/**
	 * Constructor using GeoPackage tile sizes
	 *
	 * @param tileDao
	 *            tile dao
	 */
	public GeoPackageTileRetriever(TileDao tileDao) {
		this(tileDao, null);
	}

	/**
	 * Constructor with specified tile size
	 *
	 * @param tileDao
	 *            tile dao
	 * @param imageFormat
	 *            image format
	 */
	public GeoPackageTileRetriever(TileDao tileDao, String imageFormat) {
		this(tileDao, null, null, imageFormat);
	}

	/**
	 * Constructor with specified tile size
	 *
	 * @param tileDao
	 *            tile dao
	 * @param width
	 *            width
	 * @param height
	 *            height
	 * @param imageFormat
	 *            image format
	 */
	public GeoPackageTileRetriever(TileDao tileDao, Integer width,
			Integer height, String imageFormat) {

		tileDao.adjustTileMatrixLengths();

		Projection webMercator = ProjectionFactory
				.getProjection(ProjectionConstants.EPSG_WEB_MERCATOR);

		tileCreator = new TileCreator(tileDao, width, height, webMercator,
				imageFormat);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasTile(int x, int y, int zoom) {

		// Get the bounding box of the requested tile
		GeometryEnvelope webMercatorBoundingBox = TileBoundingBoxUtils
				.getWebMercatorBoundingBox(x, y, zoom);

		boolean hasTile = tileCreator.hasTile(webMercatorBoundingBox);

		return hasTile;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public GeoPackageTile getTile(int x, int y, int zoom) {

		// Get the bounding box of the requested tile
		GeometryEnvelope webMercatorBoundingBox = TileBoundingBoxUtils
				.getWebMercatorBoundingBox(x, y, zoom);

		GeoPackageTile tile = tileCreator.getTile(webMercatorBoundingBox);

		return tile;
	}

}
