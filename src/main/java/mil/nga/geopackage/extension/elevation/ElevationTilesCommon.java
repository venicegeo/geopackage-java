package mil.nga.geopackage.extension.elevation;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.tiles.ImageRectangle;
import mil.nga.geopackage.tiles.ImageRectangleF;
import mil.nga.geopackage.tiles.TileBoundingBoxJavaUtils;
import mil.nga.geopackage.tiles.TileBoundingBoxUtils;
import mil.nga.geopackage.tiles.TileGrid;
import mil.nga.geopackage.tiles.matrix.TileMatrix;
import mil.nga.geopackage.tiles.user.TileDao;
import mil.nga.geopackage.tiles.user.TileResultSet;
import mil.nga.geopackage.tiles.user.TileRow;
import mil.nga.geopackage.tiles.user.TileTable;
import mil.nga.sf.GeometryEnvelope;
import mil.nga.sf.projection.Projection;
import mil.nga.sf.projection.ProjectionTransform;

/**
 * Tiled Gridded Elevation Common Data Extension
 * 
 * @author osbornb
 * @since 1.2.1
 */
public abstract class ElevationTilesCommon<TImage extends ElevationImage>
		extends ElevationTilesCore<TImage> {

	/**
	 * Tile DAO
	 */
	protected final TileDao tileDao;

	/**
	 * Constructor
	 * 
	 * @param geoPackage
	 *            GeoPackage
	 * @param tileDao
	 *            tile dao
	 * @param width
	 *            elevation response width
	 * @param height
	 *            elevation response height
	 * @param requestProjection
	 *            request projection
	 */
	public ElevationTilesCommon(GeoPackage geoPackage, TileDao tileDao,
			Integer width, Integer height, Projection requestProjection) {
		super(geoPackage, tileDao.getTileMatrixSet(), width, height,
				requestProjection);
		this.tileDao = tileDao;
	}

	/**
	 * Create an elevation image
	 *
	 * @param tileRow
	 *            tile row
	 * @return image
	 */
	public abstract TImage createElevationImage(TileRow tileRow);

	/**
	 * Get the elevation value of the pixel in the tile row image
	 * 
	 * @param griddedTile
	 *            gridded tile
	 * @param tileRow
	 *            tile row
	 * @param x
	 *            x coordinate
	 * @param y
	 *            y coordinate
	 * @return elevation value
	 */
	public abstract double getElevationValue(GriddedTile griddedTile,
			TileRow tileRow, int x, int y);

	/**
	 * Get the tile dao
	 * 
	 * @return tile dao
	 */
	public TileDao getTileDao() {
		return tileDao;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ElevationTileResults getElevations(ElevationRequest request,
			Integer width, Integer height) {

		ElevationTileResults elevationResults = null;

		// Transform to the projection of the elevation tiles
		ProjectionTransform transformRequestToElevation = null;
		GeometryEnvelope requestProjectedBoundingBox = request.getBoundingBox();
		if (!sameProjection) {
			transformRequestToElevation = requestProjection
					.getTransformation(elevationProjection);
			requestProjectedBoundingBox = transformRequestToElevation
					.transform(requestProjectedBoundingBox);
		}
		request.setProjectedBoundingBox(requestProjectedBoundingBox);

		// Determine how many overlapping pixels to store based upon the
		// algorithm
		int overlappingPixels;
		switch (algorithm) {
		case BICUBIC:
			overlappingPixels = 3;
			break;
		default:
			overlappingPixels = 1;
		}

		// Find the tile matrix and results
		ElevationTileMatrixResults results = getResults(request,
				requestProjectedBoundingBox, overlappingPixels);

		if (results != null) {

			TileMatrix tileMatrix = results.getTileMatrix();
			TileResultSet tileResults = results.getTileResults();

			try {

				// Determine the requested elevation dimensions, or use the
				// dimensions of a single tile matrix elevation tile
				int requestedElevationsWidth = width != null ? width
						: (int) tileMatrix.getTileWidth();
				int requestedElevationsHeight = height != null ? height
						: (int) tileMatrix.getTileHeight();

				// Determine the size of the non projected elevation results
				int tileWidth = requestedElevationsWidth;
				int tileHeight = requestedElevationsHeight;
				if (!sameProjection) {
					int projectedWidth = (int) Math
							.round((requestProjectedBoundingBox
									.getMaxLongitude() - requestProjectedBoundingBox
									.getMinLongitude())
									/ tileMatrix.getPixelXSize());
					if (projectedWidth > 0) {
						tileWidth = projectedWidth;
					}
					int projectedHeight = (int) Math
							.round((requestProjectedBoundingBox
									.getMaxLatitude() - requestProjectedBoundingBox
									.getMinLatitude())
									/ tileMatrix.getPixelYSize());
					if (projectedHeight > 0) {
						tileHeight = projectedHeight;
					}
				}

				// Retrieve the elevations from the results
				Double[][] elevations = getElevations(tileMatrix, tileResults,
						request, tileWidth, tileHeight, overlappingPixels);

				// Project the elevations if needed
				if (elevations != null && !sameProjection && !request.isPoint()) {
					elevations = reprojectElevations(elevations,
							requestedElevationsWidth,
							requestedElevationsHeight,
							request.getBoundingBox(),
							transformRequestToElevation,
							requestProjectedBoundingBox);
				}

				// Create the results
				if (elevations != null) {
					elevationResults = new ElevationTileResults(elevations,
							tileMatrix);
				}
			} finally {
				tileResults.close();
			}
		}

		return elevationResults;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ElevationTileResults getElevationsUnbounded(ElevationRequest request) {

		ElevationTileResults elevationResults = null;

		// Transform to the projection of the elevation tiles
		ProjectionTransform transformRequestToElevation = null;
		GeometryEnvelope requestProjectedBoundingBox = request.getBoundingBox();
		if (!sameProjection) {
			transformRequestToElevation = requestProjection
					.getTransformation(elevationProjection);
			requestProjectedBoundingBox = transformRequestToElevation
					.transform(requestProjectedBoundingBox);
		}
		request.setProjectedBoundingBox(requestProjectedBoundingBox);

		// Find the tile matrix and results
		ElevationTileMatrixResults results = getResults(request,
				requestProjectedBoundingBox);

		if (results != null) {

			TileMatrix tileMatrix = results.getTileMatrix();
			TileResultSet tileResults = results.getTileResults();

			try {

				// Retrieve the elevations from the results
				Double[][] elevations = getElevationsUnbounded(tileMatrix,
						tileResults, request);

				// Project the elevations if needed
				if (elevations != null && !sameProjection && !request.isPoint()) {
					elevations = reprojectElevations(elevations,
							elevations[0].length, elevations.length,
							request.getBoundingBox(),
							transformRequestToElevation,
							requestProjectedBoundingBox);
				}

				// Create the results
				if (elevations != null) {
					elevationResults = new ElevationTileResults(elevations,
							tileMatrix);
				}

			} finally {
				tileResults.close();
			}
		}

		return elevationResults;
	}

	/**
	 * Get the elevation tile results by finding the tile matrix with values
	 * 
	 * @param request
	 *            elevation request
	 * @param requestProjectedBoundingBox
	 *            request projected bounding box
	 * @return tile matrix results
	 */
	private ElevationTileMatrixResults getResults(ElevationRequest request,
			GeometryEnvelope requestProjectedBoundingBox) {
		return getResults(request, requestProjectedBoundingBox, 0);
	}

	/**
	 * Get the elevation tile results by finding the tile matrix with values
	 * 
	 * @param request
	 *            elevation request
	 * @param requestProjectedBoundingBox
	 *            request projected bounding box
	 * @param overlappingPixels
	 *            overlapping request pixels
	 * @return tile matrix results
	 */
	private ElevationTileMatrixResults getResults(ElevationRequest request,
			GeometryEnvelope requestProjectedBoundingBox, int overlappingPixels) {
		// Try to get the elevation from the current zoom level
		TileMatrix tileMatrix = getTileMatrix(request);
		ElevationTileMatrixResults results = null;
		if (tileMatrix != null) {
			results = getResults(requestProjectedBoundingBox, tileMatrix,
					overlappingPixels);

			// Try to zoom in or out to find a matching elevation
			if (results == null) {
				results = getResultsZoom(requestProjectedBoundingBox,
						tileMatrix, overlappingPixels);
			}
		}
		return results;
	}

	/**
	 * Get the elevation tile results for a specified tile matrix
	 * 
	 * @param requestProjectedBoundingBox
	 *            request projected bounding box
	 * @param tileMatrix
	 *            tile matrix
	 * @param overlappingPixels
	 *            number of overlapping pixels used by the algorithm
	 * @return tile matrix results
	 */
	private ElevationTileMatrixResults getResults(
			GeometryEnvelope requestProjectedBoundingBox, TileMatrix tileMatrix,
			int overlappingPixels) {
		ElevationTileMatrixResults results = null;
		GeometryEnvelope paddedBoundingBox = padBoundingBox(tileMatrix,
				requestProjectedBoundingBox, overlappingPixels);
		TileResultSet tileResults = retrieveSortedTileResults(
				paddedBoundingBox, tileMatrix);
		if (tileResults != null) {
			if (tileResults.getCount() > 0) {
				results = new ElevationTileMatrixResults(tileMatrix,
						tileResults);
			} else {
				tileResults.close();
			}
		}
		return results;
	}

	/**
	 * Get the elevation tile results by zooming in or out as needed from the
	 * provided tile matrix to find values
	 * 
	 * @param requestProjectedBoundingBox
	 *            request projected bounding box
	 * @param tileMatrix
	 *            tile matrix
	 * @param overlappingPixels
	 *            overlapping request pixels
	 * @return tile matrix results
	 */
	private ElevationTileMatrixResults getResultsZoom(
			GeometryEnvelope requestProjectedBoundingBox, TileMatrix tileMatrix,
			int overlappingPixels) {

		ElevationTileMatrixResults results = null;

		if (zoomIn && zoomInBeforeOut) {
			results = getResultsZoomIn(requestProjectedBoundingBox, tileMatrix,
					overlappingPixels);
		}
		if (results == null && zoomOut) {
			results = getResultsZoomOut(requestProjectedBoundingBox,
					tileMatrix, overlappingPixels);
		}
		if (results == null && zoomIn && !zoomInBeforeOut) {
			results = getResultsZoomIn(requestProjectedBoundingBox, tileMatrix,
					overlappingPixels);
		}

		return results;
	}

	/**
	 * Get the elevation tile results by zooming in from the provided tile
	 * matrix
	 * 
	 * @param requestProjectedBoundingBox
	 *            request projected bounding box
	 * @param tileMatrix
	 *            tile matrix
	 * @param overlappingPixels
	 *            overlapping request pixels
	 * @return tile matrix results
	 */
	private ElevationTileMatrixResults getResultsZoomIn(
			GeometryEnvelope requestProjectedBoundingBox, TileMatrix tileMatrix,
			int overlappingPixels) {

		ElevationTileMatrixResults results = null;
		for (long zoomLevel = tileMatrix.getZoomLevel() + 1; zoomLevel <= tileDao
				.getMaxZoom(); zoomLevel++) {
			TileMatrix zoomTileMatrix = tileDao.getTileMatrix(zoomLevel);
			if (zoomTileMatrix != null) {
				results = getResults(requestProjectedBoundingBox,
						zoomTileMatrix, overlappingPixels);
				if (results != null) {
					break;
				}
			}
		}
		return results;
	}

	/**
	 * Get the elevation tile results by zooming out from the provided tile
	 * matrix
	 * 
	 * @param requestProjectedBoundingBox
	 *            request projected bounding box
	 * @param tileMatrix
	 *            tile matrix
	 * @param overlappingPixels
	 *            overlapping request pixels
	 * @return tile matrix results
	 */
	private ElevationTileMatrixResults getResultsZoomOut(
			GeometryEnvelope requestProjectedBoundingBox, TileMatrix tileMatrix,
			int overlappingPixels) {

		ElevationTileMatrixResults results = null;
		for (long zoomLevel = tileMatrix.getZoomLevel() - 1; zoomLevel >= tileDao
				.getMinZoom(); zoomLevel--) {
			TileMatrix zoomTileMatrix = tileDao.getTileMatrix(zoomLevel);
			if (zoomTileMatrix != null) {
				results = getResults(requestProjectedBoundingBox,
						zoomTileMatrix, overlappingPixels);
				if (results != null) {
					break;
				}
			}
		}
		return results;
	}

	/**
	 * Get the elevation values from the tile results scaled to the provided
	 * dimensions
	 * 
	 * @param tileMatrix
	 *            tile matrix
	 * @param tileResults
	 *            tile results
	 * @param request
	 *            elevation request
	 * @param tileWidth
	 *            tile width
	 * @param tileHeight
	 *            tile height
	 * @param overlappingPixels
	 *            overlapping request pixels
	 * @return elevation values
	 */
	private Double[][] getElevations(TileMatrix tileMatrix,
			TileResultSet tileResults, ElevationRequest request, int tileWidth,
			int tileHeight, int overlappingPixels) {

		Double[][] elevations = null;

		// Tiles are ordered by rows and then columns. Track the last column
		// elevations of the tile to the left and the last rows of the tiles in
		// the row above
		Double[][] leftLastColumns = null;
		Map<Long, Double[][]> lastRowsByColumn = null;
		Map<Long, Double[][]> previousLastRowsByColumn = null;

		long previousRow = -1;
		long previousColumn = Long.MAX_VALUE;

		// Process each elevation tile
		while (tileResults.moveToNext()) {

			// Get the next elevation tile
			TileRow tileRow = tileResults.getRow();

			long currentRow = tileRow.getTileRow();
			long currentColumn = tileRow.getTileColumn();

			// If the row has changed, save off the previous last rows and begin
			// tracking this row. Clear the left last columns.
			if (currentRow > previousRow) {
				previousLastRowsByColumn = lastRowsByColumn;
				lastRowsByColumn = new HashMap<Long, Double[][]>();
				leftLastColumns = null;
			}

			// If there was a previous row, retrieve the top left and top
			// overlapping rows
			Double[][] topLeftRows = null;
			Double[][] topRows = null;
			if (previousLastRowsByColumn != null) {
				topLeftRows = previousLastRowsByColumn.get(currentColumn - 1);
				topRows = previousLastRowsByColumn.get(currentColumn);
			}

			// If the current column is not the column after the previous clear
			// the left values
			if (currentColumn < previousColumn
					|| currentColumn != previousColumn + 1) {
				leftLastColumns = null;
			}

			// Get the bounding box of the elevation
			GeometryEnvelope tileBoundingBox = TileBoundingBoxUtils
					.getBoundingBox(elevationBoundingBox, tileMatrix,
							currentColumn, currentRow);

			// Get the bounding box where the request and elevation tile overlap
			GeometryEnvelope overlap = request.overlap(tileBoundingBox);

			// Get the gridded tile value for the tile
			GriddedTile griddedTile = getGriddedTile(tileRow.getId());

			// Get the elevation tile image
			TImage image = createElevationImage(tileRow);

			// If the tile overlaps with the requested box
			if (overlap != null) {

				// Get the rectangle of the tile elevation with matching values
				ImageRectangleF src = TileBoundingBoxJavaUtils
						.getFloatRectangle(tileMatrix.getTileWidth(),
								tileMatrix.getTileHeight(), tileBoundingBox,
								overlap);

				// Get the rectangle of where to store the results
				ImageRectangleF dest = null;
				if (request.getProjectedBoundingBox().equals(overlap)) {
					if (request.isPoint()) {
						// For single points request only a single destination
						// pixel
						dest = new ImageRectangleF(0, 0, 0, 0);
					} else {
						// The overlap is equal to the request, set as the full
						// destination size
						dest = new ImageRectangleF(0, 0, tileWidth, tileHeight);
					}
				} else {
					dest = TileBoundingBoxJavaUtils.getFloatRectangle(
							tileWidth, tileHeight,
							request.getProjectedBoundingBox(), overlap);
				}

				if (src.isValidAllowEmpty() && dest.isValidAllowEmpty()) {

					// Create the elevations array first time through
					if (elevations == null) {
						elevations = new Double[tileHeight][tileWidth];
					}

					// Get the destination widths
					float destWidth = dest.getRight() - dest.getLeft();
					float destHeight = dest.getBottom() - dest.getTop();

					// Get the destination heights
					float srcWidth = src.getRight() - src.getLeft();
					float srcHeight = src.getBottom() - src.getTop();

					// Determine the source to destination ratio and how many
					// destination pixels equal half a source pixel
					float widthRatio;
					float halfDestWidthPixel;
					if (destWidth == 0) {
						widthRatio = 0.0f;
						halfDestWidthPixel = 0.0f;
					} else {
						widthRatio = srcWidth / destWidth;
						halfDestWidthPixel = 0.5f / widthRatio;
					}
					float heightRatio;
					float halfDestHeightPixel;
					if (destHeight == 0) {
						heightRatio = 0.0f;
						halfDestHeightPixel = 0.0f;
					} else {
						heightRatio = srcHeight / destHeight;
						halfDestHeightPixel = 0.5f / heightRatio;
					}

					float algorithmDestWidthPixelOverlap = halfDestWidthPixel
							* overlappingPixels;
					float algorithmDestHeightPixelOverlap = halfDestHeightPixel
							* overlappingPixels;

					// Determine the range of destination values to set
					int minDestY = (int) Math.floor(dest.getTop()
							- algorithmDestHeightPixelOverlap);
					int maxDestY = (int) Math.ceil(dest.getBottom()
							+ algorithmDestHeightPixelOverlap);
					int minDestX = (int) Math.floor(dest.getLeft()
							- algorithmDestWidthPixelOverlap);
					int maxDestX = (int) Math.ceil(dest.getRight()
							+ algorithmDestWidthPixelOverlap);
					minDestY = Math.max(minDestY, 0);
					minDestX = Math.max(minDestX, 0);
					maxDestY = Math.min(maxDestY, tileHeight - 1);
					maxDestX = Math.min(maxDestX, tileWidth - 1);

					// Read and set the elevation values
					for (int y = minDestY; y <= maxDestY; y++) {
						for (int x = minDestX; x <= maxDestX; x++) {

							// Determine the elevation based upon the
							// selected algorithm
							Double elevation = null;
							switch (algorithm) {
							case NEAREST_NEIGHBOR:
								elevation = getNearestNeighborElevation(
										griddedTile, image, leftLastColumns,
										topLeftRows, topRows, y, x, widthRatio,
										heightRatio, dest.getTop(),
										dest.getLeft(), src.getTop(),
										src.getLeft());
								break;
							case BILINEAR:
								elevation = getBilinearInterpolationElevation(
										griddedTile, image, leftLastColumns,
										topLeftRows, topRows, y, x, widthRatio,
										heightRatio, dest.getTop(),
										dest.getLeft(), src.getTop(),
										src.getLeft());
								break;
							case BICUBIC:
								elevation = getBicubicInterpolationElevation(
										griddedTile, image, leftLastColumns,
										topLeftRows, topRows, y, x, widthRatio,
										heightRatio, dest.getTop(),
										dest.getLeft(), src.getTop(),
										src.getLeft());
								break;
							default:
								throw new UnsupportedOperationException(
										"Algorithm is not supported: "
												+ algorithm);
							}

							if (elevation != null) {
								elevations[y][x] = elevation;
							}

						}
					}

				}
			}

			// Determine and store the elevations of the last columns and rows
			leftLastColumns = new Double[overlappingPixels][(int) tileMatrix
					.getTileHeight()];
			Double[][] lastRows = new Double[overlappingPixels][(int) tileMatrix
					.getTileWidth()];
			lastRowsByColumn.put(currentColumn, lastRows);

			// For each overlapping pixel
			for (int lastIndex = 0; lastIndex < overlappingPixels; lastIndex++) {

				// Store the last column row elevation values
				int lastColumnIndex = (int) tileMatrix.getTileWidth()
						- lastIndex - 1;
				for (int row = 0; row < tileMatrix.getTileHeight(); row++) {
					Double elevation = getElevationValue(griddedTile, image,
							lastColumnIndex, row);
					leftLastColumns[lastIndex][row] = elevation;
				}

				// Store the last row column elevation values
				int lastRowIndex = (int) tileMatrix.getTileHeight() - lastIndex
						- 1;
				for (int column = 0; column < tileMatrix.getTileWidth(); column++) {
					Double elevation = getElevationValue(griddedTile, image,
							column, lastRowIndex);
					lastRows[lastIndex][column] = elevation;
				}

			}

			// Update the previous row and column
			previousRow = currentRow;
			previousColumn = currentColumn;
		}

		return elevations;
	}

	/**
	 * Get the elevation values from the tile results unbounded in result size
	 * 
	 * @param tileMatrix
	 *            tile matrix
	 * @param tileResults
	 *            tile results
	 * @param request
	 *            elevation request
	 * @return elevation values
	 */
	private Double[][] getElevationsUnbounded(TileMatrix tileMatrix,
			TileResultSet tileResults, ElevationRequest request) {

		// Build a map of rows to maps of columns and values
		Map<Long, Map<Long, Double[][]>> rowsMap = new TreeMap<>();

		// Track the min and max row and column
		Long minRow = null;
		Long maxRow = null;
		Long minColumn = null;
		Long maxColumn = null;

		// Track count of tiles involved in the results
		int tileCount = 0;

		// Process each elevation tile row
		while (tileResults.moveToNext()) {

			// Get the next elevation tile
			TileRow tileRow = tileResults.getRow();

			// Get the bounding box of the elevation
			GeometryEnvelope tileBoundingBox = TileBoundingBoxUtils.getBoundingBox(
					elevationBoundingBox, tileMatrix, tileRow.getTileColumn(),
					tileRow.getTileRow());

			// Get the bounding box where the request and elevation tile overlap
			GeometryEnvelope overlap = request.overlap(tileBoundingBox);

			// If the elevation tile overlaps with the requested box
			if (overlap != null) {

				// Get the rectangle of the tile elevation with matching values
				ImageRectangle src = TileBoundingBoxJavaUtils.getRectangle(
						tileMatrix.getTileWidth(), tileMatrix.getTileHeight(),
						tileBoundingBox, overlap);

				if (src.isValidAllowEmpty()) {

					// Get the source dimensions
					int srcTop = Math.min(src.getTop(),
							(int) tileMatrix.getTileHeight() - 1);
					int srcBottom = Math.min(src.getBottom(),
							(int) tileMatrix.getTileHeight() - 1);
					int srcLeft = Math.min(src.getLeft(),
							(int) tileMatrix.getTileWidth() - 1);
					int srcRight = Math.min(src.getRight(),
							(int) tileMatrix.getTileWidth() - 1);

					// Get the gridded tile value for the tile
					GriddedTile griddedTile = getGriddedTile(tileRow.getId());

					// Get the elevation tile image
					TImage image = createElevationImage(tileRow);

					// Create the elevation results for this tile
					Double[][] elevations = new Double[srcBottom - srcTop + 1][srcRight
							- srcLeft + 1];

					// Get or add the columns map to the rows map
					Map<Long, Double[][]> columnsMap = rowsMap.get(tileRow
							.getTileRow());
					if (columnsMap == null) {
						columnsMap = new TreeMap<Long, Double[][]>();
						rowsMap.put(tileRow.getTileRow(), columnsMap);
					}

					// Read and set the elevation values
					for (int y = srcTop; y <= srcBottom; y++) {

						for (int x = srcLeft; x <= srcRight; x++) {

							// Get the elevation value from the source pixel
							Double elevation = getElevationValue(griddedTile,
									image, x, y);

							elevations[y - srcTop][x - srcLeft] = elevation;
						}
					}

					// Set the elevations in the results map
					columnsMap.put(tileRow.getTileColumn(), elevations);

					// Increase the contributing tiles count
					tileCount++;

					// Track the min and max row and column
					minRow = minRow == null ? tileRow.getTileRow() : Math.min(
							minRow, tileRow.getTileRow());
					maxRow = maxRow == null ? tileRow.getTileRow() : Math.max(
							maxRow, tileRow.getTileRow());
					minColumn = minColumn == null ? tileRow.getTileColumn()
							: Math.min(minColumn, tileRow.getTileColumn());
					maxColumn = maxColumn == null ? tileRow.getTileColumn()
							: Math.max(maxColumn, tileRow.getTileColumn());
				}
			}
		}

		// Handle formatting the results
		Double[][] elevations = formatUnboundedResults(tileMatrix, rowsMap,
				tileCount, minRow, maxRow, minColumn, maxColumn);

		return elevations;
	}

	/**
	 * Get the tile matrix for the zoom level as defined by the area of the
	 * request
	 *
	 * @param request
	 *            elevation request
	 * @return tile matrix or null
	 */
	private TileMatrix getTileMatrix(ElevationRequest request) {

		TileMatrix tileMatrix = null;

		// Check if the request overlaps elevation bounding box
		if (request.overlap(elevationBoundingBox) != null) {

			// Get the tile distance
			GeometryEnvelope projectedBoundingBox = request
					.getProjectedBoundingBox();
			double distanceWidth = projectedBoundingBox.getMaxLongitude()
					- projectedBoundingBox.getMinLongitude();
			double distanceHeight = projectedBoundingBox.getMaxLatitude()
					- projectedBoundingBox.getMinLatitude();

			// Get the zoom level to request based upon the tile size
			Long zoomLevel = tileDao.getClosestZoomLevel(distanceWidth,
					distanceHeight);

			// If there is a matching zoom level
			if (zoomLevel != null) {
				tileMatrix = tileDao.getTileMatrix(zoomLevel);
			}
		}

		return tileMatrix;
	}

	/**
	 * Get the tile row results of elevation tiles needed to create the
	 * requested bounding box elevations, sorted by row and then column
	 *
	 * @param projectedRequestBoundingBox
	 *            bounding box projected to the elevations
	 * @param tileMatrix
	 *            tile matrix
	 * @return tile results or null
	 */
	private TileResultSet retrieveSortedTileResults(
			GeometryEnvelope projectedRequestBoundingBox, TileMatrix tileMatrix) {

		TileResultSet tileResults = null;

		if (tileMatrix != null) {

			// Get the tile grid
			TileGrid tileGrid = TileBoundingBoxUtils.getTileGrid(
					elevationBoundingBox, tileMatrix.getMatrixWidth(),
					tileMatrix.getMatrixHeight(), projectedRequestBoundingBox);

			// Query for matching tiles in the tile grid
			tileResults = tileDao.queryByTileGrid(tileGrid,
					tileMatrix.getZoomLevel(), TileTable.COLUMN_TILE_ROW + ","
							+ TileTable.COLUMN_TILE_COLUMN);

		}

		return tileResults;
	}

	/**
	 * Get the elevation value of the pixel in the tile row image
	 * 
	 * @param tileRow
	 *            tile row
	 * @param x
	 *            x coordinate
	 * @param y
	 *            y coordinate
	 * @return elevation value
	 */
	public double getElevationValue(TileRow tileRow, int x, int y) {
		GriddedTile griddedTile = getGriddedTile(tileRow.getId());
		double elevation = getElevationValue(griddedTile, tileRow, x, y);
		return elevation;
	}

}
