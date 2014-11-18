package org.blep.jpa.kata;

import com.google.common.base.Stopwatch;
import io.blep.spysql.SqlCounter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.io.IOUtils;
import org.blep.jpa.JpaBaseTest;
import org.hibernate.jpa.QueryHints;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Generated;
import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static java.lang.System.nanoTime;
import static org.blep.jpa.kata._09Interrogations.User.BY_USERNAME;
import static org.fest.assertions.Assertions.assertThat;

/**
 * User: blep
 * Date: 29/08/14
 * Time: 18:52
 */

public class _09Interrogations extends JpaBaseTest implements JpaBaseTest.ServerDataStore{

    private final String loremIpsum = readLorem();
    private EntityManager em;

    @Override
    protected List<Class<?>> persistentClasses() {
        return Arrays.asList(User.class);
    }

    @Entity
    @Data @EqualsAndHashCode(exclude = "id")
    @NamedQuery(name = BY_USERNAME,
            query = "select u from org.blep.jpa.kata._09Interrogations$User u where u.name = :name")
    public static class User {
        public static final String BY_USERNAME = "byUserName";
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private Long id;

        private String name;
    }

    @StaticMetamodel(User.class)
    public static class User_{
        public static volatile SingularAttribute<User,Long> id;
        public static volatile SingularAttribute<User,String> name;
    }

    @Test
    public void testQueryString() throws Exception {
        System.out.println("_09Interrogations.testQueryString");
        final String criteria = "bali balo";

        // Do not ever do this!!
        final List<User> users =
                em.createQuery("select u from org.blep.jpa.kata._09Interrogations$User u where u.name = '" + criteria+"'"
                , User.class)
                .getResultList();

        assertThat(users).hasSize(1);
    }

    @Test
    public void testQueryParametrizedString() throws Exception {
        System.out.println("_09Interrogations.testQueryParametrizedString");
        final String criteria = "bali balo";

        // Avoid doing this...
        final List<User> users =
                em.createQuery("select u from org.blep.jpa.kata._09Interrogations$User u where u.name = :name"
                    , User.class)
                .setParameter("name", criteria)
                .getResultList();

        assertThat(users).hasSize(1);
    }

    @Test
    public void testNamedQuery() throws Exception {
        System.out.println("_09Interrogations.testNamedQuery");

        final String criteria = "bali balo";
        final List<User> users = em.createNamedQuery(User.BY_USERNAME, User.class)
                .setParameter("name", criteria)
                .getResultList();

        assertThat(users).hasSize(1);
    }

    @Test
    public void testUntypedCriteriaQuery() throws Exception {
        System.out.println("_09Interrogations.testMetaModelCriteriaQuery");

        final String criteria = "bali balo";

        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<User> q = cb.createQuery(User.class);
        final Root<User> root = q.from(User.class);

        q.select(root)
            .where(
                    cb.equal(root.get("name"),criteria)
            );

        final List<User> users = em.createQuery(q).getResultList();

        assertThat(users).hasSize(1);

    }

    @Test
    public void testMetaModelCriteriaQuery() throws Exception {
        System.out.println("_09Interrogations.testMetaModelCriteriaQuery");

        final String criteria = "bali balo";

        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<User> q = cb.createQuery(User.class);
        final Root<User> root = q.from(User.class);

        q.select(root)
            .where(
                    cb.equal(root.get(User_.name),criteria)
            );

        final List<User> users = em.createQuery(q).getResultList();

        assertThat(users).hasSize(1);

    }

    @Before
    public void setUp() throws Exception {
        em = emf.createEntityManager();
        final User user = new User();
        user.setName("bali balo");
        em.getTransaction().begin();
        em.persist(user);
        em.getTransaction().commit();
        em.clear();
    }

    @After
    public void tearDown() throws Exception {
        em.getTransaction().begin();
        em.createQuery("delete from org.blep.jpa.kata._09Interrogations$User").executeUpdate();
        em.getTransaction().commit();
        em.close();
        emf.close();
    }




    @Override
    protected Properties config() {
        final Properties config = super.config();
//        config.put("hibernate.show_sql", true);
//        config.put("hibernate.format_sql", true);
        return config;
    }


}
