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

import javax.persistence.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static java.lang.System.nanoTime;
import static org.fest.assertions.Assertions.assertThat;

/**
 * User: blep
 * Not used because not the difference can't be seen with a micro environment
 */

public class _08SelectAll extends JpaBaseTest implements JpaBaseTest.ServerDataStore{

    private final String loremIpsum = readLorem();
    private final int total = 50000;
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

/*
        @OneToOne(fetch = FetchType.LAZY)
        private Biography biography;
*/
    }

    @Entity
    @Data @EqualsAndHashCode(exclude = "id")
    public static class Biography{
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private Long id;

        @Column(length = 4000)
        private String content;

    }


    @Test
    public void testQueryAllUser() throws Exception {

        final SqlCounter counter = new SqlCounter();
        ds.addListener(counter);

        for(int i = 0;i<5; i++){
            final Stopwatch watch = Stopwatch.createStarted();
/*
            final TypedQuery<User> query = em.createQuery("select u from "+getClass().getName()+"$User u",
                    User.class);
*/
            final TypedQuery<Biography> query = em.createQuery("select u from "+getClass().getName()+"$Biography u",
                    Biography.class);
            query.setHint(QueryHints.HINT_FETCH_SIZE,1);


            final List<Biography> all = query.getResultList();
            System.out.println("watch.elapsed(TimeUnit.MILLISECONDS) = " + watch.elapsed(TimeUnit.MILLISECONDS));

            System.out.println("all.size() = " + all.size());
            System.out.println("listener.getCount() = " + counter.getCount());

            assertThat(counter.getCount()).isEqualTo(1);

            counter.reset();
            em.clear();
        }

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
        final Stopwatch watch = Stopwatch.createStarted();
        em.getTransaction().begin();

        for(int i=0; i< total; i++) {
            final User user = new User();
            user.setName("bali balo" + nanoTime());
            final Biography biography = new Biography();
            biography.setContent(loremIpsum + nanoTime() + loremIpsum + loremIpsum);
//            user.setBiography(biography);
//            em.persist(user);
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

    @Override
    protected Properties config() {
        final Properties config = super.config();
//        config.put("hibernate.show_sql", true);
//        config.put("hibernate.format_sql", true);
        return config;
    }

    private void cleanUpFixture() {
        em.getTransaction().begin();
        em.createQuery("delete from "+getClass().getName()+"$User").executeUpdate();
        em.createQuery("delete from " + getClass().getName() + "$Biography").executeUpdate();
        em.getTransaction().commit();
    }
}
