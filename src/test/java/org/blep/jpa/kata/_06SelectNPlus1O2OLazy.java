package org.blep.jpa.kata;

import com.google.common.base.Stopwatch;
import io.blep.spysql.SqlCounter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.io.IOUtils;
import org.blep.jpa.JpaBaseTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.lang.System.nanoTime;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static javax.persistence.CascadeType.PERSIST;
import static org.fest.assertions.Assertions.assertThat;

/**
 * User: blep
 * Date: 29/08/14
 * Time: 18:52
 */

public class _06SelectNPlus1O2OLazy extends JpaBaseTest implements JpaBaseTest.DiskDataSource{

    private final String loremIpsum = readLorem();
    private final int total = 20000;
    private EntityManager em;

    @Override
    protected List<Class<?>> persistentClasses() {
        return Arrays.asList(User.class, Biography.class);
    }

    @Entity
    @Data @EqualsAndHashCode(exclude = "id")
    public static class User {
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private Long id;

        private String name;

        @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
        private Biography biography;
    }

    @Entity
    @Data @EqualsAndHashCode(exclude = "id")
    public static class Biography{
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private Long id;

        @Column(length = 4000)
        private String content;

        @OneToOne
        private User user;
    }


    @Test
    public void testQueryUser() throws Exception {
        final SqlCounter counter = new SqlCounter();
        ds.addListener(counter);

        final String qlString = "select u from " + getClass().getName() + "$User u";
        System.err.println("qlString = " + qlString);
        final TypedQuery<User> query = em.createQuery(qlString,
                User.class);

        final List<User> all = query.getResultList();

        System.out.println("all.size() = " + all.size());
        System.out.println("listener.getCount() = " + counter.getCount());

        assertThat(all).hasSize(1);
        assertThat(counter.getCount()).isEqualTo(1);

    }


    @Before
    public void setUp() throws Exception {
        em = emf.createEntityManager();
        insertFixture();
    }

    @After
    public void tearDown() throws Exception {
        cleanUpFixture();
        em.close();
        emf.close();
    }


    private void insertFixture() throws Exception {
        em.getTransaction().begin();
        final User user = new User();
        user.setName("bali balo" + nanoTime());
        final Biography biography = new Biography();
        biography.setContent(loremIpsum + nanoTime() + loremIpsum + loremIpsum);
        biography.setUser(user);
        em.persist(user);
        em.persist(biography);
        em.getTransaction().commit();
        em.clear();

    }

    @Override
    protected Properties config() {
        final Properties config = super.config();
        config.put("hibernate.show_sql", true);
        config.put("hibernate.format_sql", true);
        return config;
    }

    private void cleanUpFixture() {
        em.getTransaction().begin();
        em.createQuery("delete from "+getClass().getName()+"$Biography").executeUpdate();
        em.createQuery("delete from "+getClass().getName()+"$User").executeUpdate();
        em.getTransaction().commit();
    }
}
