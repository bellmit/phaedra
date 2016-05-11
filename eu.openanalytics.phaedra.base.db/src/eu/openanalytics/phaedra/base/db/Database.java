package eu.openanalytics.phaedra.base.db;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.jpa.osgi.PersistenceProvider;
import org.eclipselink.persistence.core.PatchedEclipselinkService;

import eu.openanalytics.phaedra.base.db.jpa.JPASessionCustomizer;
import eu.openanalytics.phaedra.base.db.jpa.LockingEntityManager;
import eu.openanalytics.phaedra.base.db.jpa.PersistenceXMLClassLoader;
import eu.openanalytics.phaedra.base.db.pool.ConnectionPoolManager;
import eu.openanalytics.phaedra.base.db.prefs.Prefs;

/**
 * This class is the entry point for database connectivity.
 * It immediately sets up one or more JDBC connections, plus a JPA EntityManager.
 */
public class Database {

	private ConnectionPoolManager connectionPoolManager;
	private EntityManagerFactory entityManagerFactory;
	private EntityManager entityManager;
	
	public Database(String url, String user, String password) {
		JDBCUtils.checkDbType(url);
		
		connectionPoolManager = new ConnectionPoolManager(url, user, password);
		try {
			connectionPoolManager.startup();
		} catch (SQLException e) {
			throw new RuntimeException("Failed to open database connection", e);
		}
		
		entityManagerFactory = new PersistenceProvider().createEntityManagerFactory(PersistenceXMLClassLoader.MODEL_NAME, createJPAProperties());
		entityManager = new LockingEntityManager(entityManagerFactory.createEntityManager());
		
		PatchedEclipselinkService.getInstance().setEntityManagerLock(((LockingEntityManager)entityManager).getLock());
	}
	
	public ConnectionPoolManager getConnectionPoolManager() {
		return connectionPoolManager;
	}
	
	public EntityManager getEntityManager() {
		return entityManager;
	}

	public void close() {
		try { entityManagerFactory.close(); } catch (Exception e) {}
		try { connectionPoolManager.close(); } catch (Exception e) {}
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	private Map<String, Object> createJPAProperties() {
		int totalConnections = Activator.getDefault().getPreferenceStore().getInt(Prefs.DB_POOL_SIZE);
		int jpaConnections = Math.max(1, totalConnections / 2);
		
		Map<String, Object> properties = new HashMap<String, Object>();

		properties.put(PersistenceUnitProperties.TRANSACTION_TYPE, "RESOURCE_LOCAL");
		properties.put(PersistenceUnitProperties.CLASSLOADER, new PersistenceXMLClassLoader(this.getClass().getClassLoader()));
		
		properties.put(PersistenceUnitProperties.CONNECTION_POOL + "initial", String.valueOf(jpaConnections));
		properties.put(PersistenceUnitProperties.CONNECTION_POOL + "min", String.valueOf(jpaConnections));
		properties.put(PersistenceUnitProperties.CONNECTION_POOL + "max", String.valueOf(jpaConnections));
		properties.put(PersistenceUnitProperties.SESSION_CUSTOMIZER, JPASessionCustomizer.class.getName());
		properties.put(JPASessionCustomizer.PROP_CONNECTION_POOL, connectionPoolManager);
		
		properties.put(PersistenceUnitProperties.LOGGING_LEVEL, "severe");
		properties.put(PersistenceUnitProperties.LOGGING_TIMESTAMP, "false");
		properties.put(PersistenceUnitProperties.LOGGING_SESSION, "false");
		properties.put(PersistenceUnitProperties.LOGGING_THREAD, "false");

		JDBCUtils.customizeJPASettings(properties);
		
		return properties;
	}
}
