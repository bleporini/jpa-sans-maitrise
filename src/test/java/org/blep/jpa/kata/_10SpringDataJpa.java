package org.blep.jpa.kata;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fest.assertions.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.Param;
import org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.*;
import javax.sql.DataSource;
import javax.transaction.Transactional;

import static org.blep.jpa.kata._09Interrogations.User.BY_USERNAME;
import static org.fest.assertions.Assertions.assertThat;

/**
 * User: blep
 * Date: 15/09/14
 * Time: 08:39
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = _10SpringDataJpa.TestAppConfig.class)
public class _10SpringDataJpa {

    @Entity
    @Data
    @EqualsAndHashCode(exclude = "id")
    public static class User {
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private Long id;

        private String name;
    }

    @Repository
    public static interface UserRepository extends JpaRepository<User,Long>{
        User findByName(String name);

        @org.springframework.data.jpa.repository.Query(
                "select u from org.blep.jpa.kata._10SpringDataJpa$User u where u.name = :name"
        )
        User trouveParNom(@Param("name") String nom);
    }



    @Autowired
    private UserRepository userRepository;

    @Test
    @Transactional
    public void testUserRepository() throws Exception {

        final User user = new User();

        user.setName("bali balo");
        userRepository.saveAndFlush(user);
        assertThat(user.getId()).isNotNull();

        final User byName = userRepository.findByName("bali balo");

        assertThat(byName.getId()).isEqualTo(user.getId());

        final User parNom = userRepository.trouveParNom("bali balo");
        assertThat(parNom.getId()).isEqualTo(user.getId());
    }

    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = org.blep.jpa.kata._10SpringDataJpa.class, considerNestedRepositories = true)
    @ComponentScan(basePackageClasses = org.blep.jpa.kata._10SpringDataJpa.class)
    public static class TestAppConfig{
        @Bean
        public DataSource dataSource() {
            return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
                    .build();
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            return new JpaTransactionManager( entityManagerFactory() );
        }

        @Bean
        public EntityManagerFactory entityManagerFactory() {
            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            vendorAdapter.setGenerateDdl(Boolean.TRUE);
            vendorAdapter.setShowSql(Boolean.TRUE);
            LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
            factory.setJpaVendorAdapter(vendorAdapter);
            factory.setPackagesToScan("org.blep.jpa.kata");
            factory.setDataSource(dataSource());
            factory.afterPropertiesSet();
            factory.setLoadTimeWeaver(new InstrumentationLoadTimeWeaver());
            return factory.getObject();
        }

    }
}
