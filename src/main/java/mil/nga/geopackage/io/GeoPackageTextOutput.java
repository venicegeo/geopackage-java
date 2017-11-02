package mil.nga.geopackage.io;

import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.core.contents.Contents;
import mil.nga.geopackage.core.srs.SpatialReferenceSystem;
import mil.nga.sf.GeometryEnvelope;

/**
 * Builds readable text output from a GeoPackage
 * 
 * @author osbornb
 * @since 1.1.2
 */
public class GeoPackageTextOutput {

	/**
	 * GeoPackage
	 */
	private final GeoPackage geoPackage;

	/**
	 * Constructor
	 * 
	 * @param geoPackage
	 */
	public GeoPackageTextOutput(GeoPackage geoPackage) {
		this.geoPackage = geoPackage;
	}

	/**
	 * Get the GeoPackage file and name header text
	 * 
	 * @return text
	 */
	public String header() {
		StringBuilder output = new StringBuilder();
		output.append("GeoPackage File: " + geoPackage.getPath());
		output.append("\nGeoPackage Name: " + geoPackage.getName());
		return output.toString();
	}

	/**
	 * Text output from a SRS
	 * 
	 * @param srs
	 *            spatial reference system
	 * @return text
	 */
	public String textOutput(mil.nga.sf.srs.SpatialReferenceSystem srs) {
		StringBuilder output = new StringBuilder();
		output.append("\tSRS " + SpatialReferenceSystem.COLUMN_ORGANIZATION
				+ ": " + srs.getOrganization());
		output.append("\n\tSRS "
				+ SpatialReferenceSystem.COLUMN_ORGANIZATION_COORDSYS_ID + ": "
				+ srs.getOrganizationCoordsysId());
		output.append("\n\tSRS " + SpatialReferenceSystem.COLUMN_DEFINITION
				+ ": " + srs.getDefinition());
		return output.toString();
	}

	/**
	 * Text output from a Contents
	 * 
	 * @param contents
	 *            contents
	 * @return text
	 */
	public String textOutput(Contents contents) {
		StringBuilder output = new StringBuilder();
		output.append("\t" + Contents.COLUMN_TABLE_NAME + ": "
				+ contents.getTableName());
		output.append("\n\t" + Contents.COLUMN_DATA_TYPE + ": "
				+ contents.getDataType());
		output.append("\n\t" + Contents.COLUMN_IDENTIFIER + ": "
				+ contents.getIdentifier());
		output.append("\n\t" + Contents.COLUMN_DESCRIPTION + ": "
				+ contents.getDescription());
		output.append("\n\t" + Contents.COLUMN_LAST_CHANGE + ": "
				+ contents.getLastChange());
		output.append("\n\t" + Contents.COLUMN_MIN_X + ": "
				+ contents.getMinX());
		output.append("\n\t" + Contents.COLUMN_MIN_Y + ": "
				+ contents.getMinY());
		output.append("\n\t" + Contents.COLUMN_MAX_X + ": "
				+ contents.getMaxX());
		output.append("\n\t" + Contents.COLUMN_MAX_Y + ": "
				+ contents.getMaxY());
		output.append("\n" + textOutput(contents.getSrs()));
		return output.toString();
	}

	/**
	 * Text output from a bounding box
	 * 
	 * @param boundingBox
	 *            bounding box
	 * @return text
	 */
	public String textOutput(GeometryEnvelope boundingBox) {
		StringBuilder output = new StringBuilder();
		output.append("\tMin Longitude: " + boundingBox.getMinLongitude());
		output.append("\n\tMin Latitude: " + boundingBox.getMinLatitude());
		output.append("\n\tMax Longitude: " + boundingBox.getMaxLongitude());
		output.append("\n\tMax Latitude: " + boundingBox.getMaxLatitude());
		return output.toString();
	}

}
