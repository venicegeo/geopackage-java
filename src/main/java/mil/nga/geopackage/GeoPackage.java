package mil.nga.geopackage;

import java.sql.ResultSet;

import mil.nga.geopackage.attributes.AttributesDao;
import mil.nga.geopackage.core.contents.Contents;
import mil.nga.geopackage.features.columns.GeometryColumns;
import mil.nga.geopackage.features.user.FeatureDao;

/**
 * GeoPackage database connection
 * 
 * @author osbornb
 */
public interface GeoPackage extends GeoPackageCore {

	/**
	 * Get a Feature DAO from Geometry Columns
	 *
	 * @param geometryColumns
	 *            geometry columns
	 * @return feature dao
	 */
	public FeatureDao getFeatureDao(GeometryColumns geometryColumns);

	/**
	 * Get a Feature DAO from Contents
	 *
	 * @param contents
	 *            contents
	 * @return feature dao
	 */
	public FeatureDao getFeatureDao(Contents contents);

	/**
	 * Get a Feature DAO from a table name
	 *
	 * @param tableName
	 *            table name
	 * @return feature dao
	 */
	public FeatureDao getFeatureDao(String tableName);

	/**
	 * Get an Attributes DAO from Contents
	 * 
	 * @param contents
	 *            contents
	 * @return attributes dao
	 * @since 1.2.1
	 */
	public AttributesDao getAttributesDao(Contents contents);

	/**
	 * Get an Attributes DAO from a table name
	 * 
	 * @param tableName
	 *            table name
	 * @return attributes dao
	 * @since 1.2.1
	 */
	public AttributesDao getAttributesDao(String tableName);

	/**
	 * Perform a query on the database
	 *
	 * @param sql
	 * @param args
	 * @return result set
	 * @since 1.1.2
	 */
	public ResultSet query(String sql, String[] args);

	/**
	 * Perform a foreign key check on the database
	 *
	 * @return null if check passed, open result set with results if failed
	 * @since 1.1.2
	 */
	public ResultSet foreignKeyCheck();

	/**
	 * Perform an integrity check on the database
	 *
	 * @return null if check passed, open result set with results if failed
	 * @since 1.1.2
	 */
	public ResultSet integrityCheck();

	/**
	 * Perform a quick integrity check on the database
	 *
	 * @return null if check passed, open result set with results if failed
	 * @since 1.1.2
	 */
	public ResultSet quickCheck();

}
