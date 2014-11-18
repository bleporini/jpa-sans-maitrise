package org.blep.jpa;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import io.blep.spysql.SpyDataSource;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.IOUtils;
import org.hibernate.jpa.HibernatePersistenceProvider;

import javax.persistence.EntityManagerFactory;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

import static com.google.common.base.Throwables.propagate;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static javax.persistence.spi.PersistenceUnitTransactionType.RESOURCE_LOCAL;

/**
 * User: blep
 * Date: 29/08/14
 * Time: 08:50
 */

public abstract class JpaBaseTest implements DataSourceProvider {

    private final PersistenceProvider persistenceProvider = new HibernatePersistenceProvider();


    protected final SpyDataSource ds = buildDataSource();

    protected abstract List<Class<?>> persistentClasses();

    protected String readLorem()  {
        try(final InputStream is = getClass().getResourceAsStream("/loremipsum.txt")) {
            return IOUtils.readLines(is, Charset.forName("utf8")).get(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    public static interface DiskDataSource extends DataSourceProvider{
        /**
         * Builds a DataSource with a disk data store
         */
        default SpyDataSource buildDataSource() {
            return new SpyDataSource(buildBaseDataSource("jdbc:h2:/tmp/sos14"));
        }
    }

    public static interface MemoryDataStore extends DataSourceProvider{
        default SpyDataSource buildDataSource() {
            return new SpyDataSource(buildBaseDataSource("jdbc:h2:mem:sos14"));
        }

    }

    public static interface ServerDataStore extends DataSourceProvider{
        default SpyDataSource buildDataSource() {
            return new SpyDataSource(buildBaseDataSource("jdbc:h2:tcp://127.0.0.1:9092/sos14"));
        }

    }

    public static interface C3P0ServerDataStore extends DataSourceProvider{
        default SpyDataSource buildDataSource() {
            final ComboPooledDataSource ds = new ComboPooledDataSource();
            try {
                ds.setDriverClass("org.h2.Driver");
            } catch (PropertyVetoException e) {
                propagate(e);
            }
            ds.setJdbcUrl("jdbc:h2:tcp://127.0.0.1:9092/sos14");
            ds.setUser("sa");
            ds.setPassword("");
            ds.setMaxStatementsPerConnection(4);
            return new SpyDataSource(ds);
        }

    }

    private static DataSource buildBaseDataSource(String url) {
        BasicDataSource datasource = new BasicDataSource();
        datasource.setDriverClassName("org.h2.Driver");
        datasource.setUrl(url);
        datasource.setUsername("sa");
        datasource.setPassword("");
        datasource.setMaxActive(4);
        datasource.setPoolPreparedStatements(true);
        return datasource;
    }

    protected Properties config(){
        final Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", "create");
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        return properties;
    }

    private final PersistenceUnitInfo info =  new PersistenceUnitInfo(){




        @Override
        public String getPersistenceUnitName() {
            return "my pu";
        }

        @Override
        public String getPersistenceProviderClassName() {
            return "org.hibernate.jpa.HibernatePersistenceProvider";
        }

        @Override
        public PersistenceUnitTransactionType getTransactionType() {
            return RESOURCE_LOCAL;
        }

        @Override
        public DataSource getJtaDataSource() {
            return null;
        }

        @Override
        public DataSource getNonJtaDataSource() {
            return ds;
        }

        @Override
        public List<String> getMappingFileNames() {
            return emptyList();
        }

        @Override
        public List<URL> getJarFileUrls() {
            return emptyList();
        }

        @Override
        public URL getPersistenceUnitRootUrl() {
            return null;
        }

        @Override
        public List<String> getManagedClassNames() {
            return persistentClasses().stream().map(c -> c.getName()).collect(toList());
        }

        @Override
        public boolean excludeUnlistedClasses() {
            return false;
        }

        @Override
        public SharedCacheMode getSharedCacheMode() {
            return SharedCacheMode.NONE;
        }

        @Override
        public ValidationMode getValidationMode() {
            return ValidationMode.NONE;
        }

        @Override
        public Properties getProperties() {
            return config();
        }

        @Override
        public String getPersistenceXMLSchemaVersion() {
            return "2.1";
        }

        @Override
        public ClassLoader getClassLoader() {
            return this.getClass().getClassLoader();
        }

        @Override
        public void addTransformer(ClassTransformer transformer) {

        }

        @Override
        public ClassLoader getNewTempClassLoader() {
            throw new UnsupportedOperationException();
        }
    };

    public final EntityManagerFactory emf = persistenceProvider.createContainerEntityManagerFactory(info,new HashMap<>());


}
