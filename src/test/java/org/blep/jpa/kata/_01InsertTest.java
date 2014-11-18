package org.blep.jpa.kata;

import com.google.common.base.Stopwatch;
import io.blep.spysql.SqlCounter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.blep.jpa.JpaBaseTest;
import org.fest.assertions.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.lang.System.nanoTime;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static javax.persistence.CascadeType.PERSIST;
import static org.blep.jpa.JpaBaseTest.ServerDataStore;
import static org.fest.assertions.Assertions.assertThat;

/**
 * User: blep
 * Date: 29/08/14
 * Time: 18:52
 */

public class _01InsertTest extends JpaBaseTest implements ServerDataStore{

    private final int total = 20000;
    private EntityManager em;

    @Entity
    @Data @EqualsAndHashCode(exclude = "id")
    public static class User {
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        private String name;

        @OneToOne(mappedBy = "user")
        private Biography biography;
    }

    @Entity
    @Data @EqualsAndHashCode(exclude = "id")
    public static class Biography{
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        @Column(length = 4000)
        private String content;

        @OneToOne(cascade = PERSIST)
        private User user;
    }



    @Test
    public void testInsertFail() throws Exception {
        final Stopwatch watch = Stopwatch.createStarted();

        em.getTransaction().begin();

        for(int i=0; i< total; i++) {
            final User user = new User();
            user.setName("bali balo" + nanoTime());
            final Biography biography = new Biography();
            biography.setContent(loremIpsum + nanoTime() + loremIpsum + loremIpsum);
            biography.setUser(user);
            em.persist(user);
            em.persist(biography);
            if(i%1000==0) {
                System.out.println("i = " + i);
            }
        }
        em.getTransaction().commit();
        System.out.println("watch.elapsed(TimeUnit.MILLISECONDS) = " + watch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Test
    public void testInsertOk() throws Exception {
        final Stopwatch watch = Stopwatch.createStarted();
        em.getTransaction().begin();

        for(int i=0; i< total; i++) {
            final User user = new User();
            user.setName("bali balo" + nanoTime());
            final Biography biography = new Biography();
            biography.setContent(loremIpsum + nanoTime() + loremIpsum + loremIpsum);
            biography.setUser(user);
            em.persist(user);
            em.persist(biography);
            if(i%1000==0) {
                em.flush();
                em.clear();
                System.out.println("i = " + i);
            }
        }
        em.getTransaction().commit();
        System.out.println("watch.elapsed(TimeUnit.MILLISECONDS) = " + watch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Test
    public void testTwoSessionsFail() throws Exception {

        final User user1 = new User();
        user1.setName("bali");
        em.getTransaction().begin();
        em.persist(user1);
        em.getTransaction().commit();

        final EntityManager em2 = emf.createEntityManager();
        em2.getTransaction().begin();
        final User user2 = em2.find(User.class, user1.getId());
        user2.setName("balo");
        em2.getTransaction().commit();

        final String qlString = "select u from " + getClass().getName()
                + "$User u where u.name = :name";
        System.err.println("qlString = " + qlString);
        final User user3 = em.createQuery(qlString, User.class)
                .setParameter("name", "balo").getSingleResult();
        assertThat(user3.getName()).isEqualTo(user2.getName());

        em2.close();
    }



    @Before
    public void setUp() throws Exception {
        em = emf.createEntityManager();
    }

    @After
    public void tearDown() throws Exception {
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


    @Override
    protected List<Class<?>> persistentClasses() {
        return Arrays.asList(User.class, Biography.class);
    }

    private final String loremIpsum = readLorem();

}
