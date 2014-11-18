package org.blep.jpa.kata;

import com.google.common.base.Stopwatch;
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
import java.util.concurrent.TimeUnit;

import static java.lang.System.nanoTime;
import static javax.persistence.CascadeType.PERSIST;

/**
 * User: blep
 * Date: 29/08/14
 * Time: 18:52
 */

public class _04InsertByBatchesSequenceOrderedTest extends JpaBaseTest implements JpaBaseTest.ServerDataStore{

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

        @OneToOne(mappedBy = "user")
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

        @OneToOne(cascade = PERSIST)
        private User user;
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

    @Before
    public void setUp() throws Exception {
        em = emf.createEntityManager();
    }

    @After
    public void tearDown() throws Exception {
        em.close();
    }

    @Override
    protected Properties config() {
        final Properties config = super.config();
        config.setProperty("hibernate.jdbc.batch_size", "1000");
        config.setProperty("hibernate.order_inserts", "true");
        return config;
    }




}
