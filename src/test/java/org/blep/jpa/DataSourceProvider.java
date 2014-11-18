package org.blep.jpa;

import io.blep.spysql.SpyDataSource;

public interface DataSourceProvider {
        SpyDataSource buildDataSource();
}