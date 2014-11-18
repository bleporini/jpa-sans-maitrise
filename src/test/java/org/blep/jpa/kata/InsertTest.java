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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.System.nanoTime;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.fest.assertions.Assertions.assertThat;

/**
 * User: blep
 * Date: 29/08/14
 * Time: 18:52
 */

public class InsertTest extends JpaBaseTest implements JpaBaseTest.DiskDataSource{

    private final String loremIpsum = readLorem();
    private int total;
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

//        @OneToOne(fetch = FetchType.LAZY,mappedBy = "user")
        @OneToOne(fetch = FetchType.LAZY)
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

//        @OneToOne(cascade = PERSIST)
//        @OneToOne(mappedBy = "biography",cascade = PERSIST)
//        private User user;
    }

    @Before
    public void setUp() throws Exception {
        em = emf.createEntityManager();
    }

    @After
    public void tearDown() throws Exception {
        em.close();
    }


    @Test
    public void testInsert() throws Exception {
        em.getTransaction().begin();

        total = 200000;
        for(int i=0; i< total; i++) {
            final User user = new User();
            user.setName("bali balo" + nanoTime());
            final Biography biography = new Biography();
            biography.setContent(loremIpsum + nanoTime() + loremIpsum + loremIpsum);
//            biography.setUser(user);
            em.persist(user);
            em.persist(biography);
            if(i%1000==0) {
                System.out.println("i = " + i);
                em.clear();
            }
        }
        em.getTransaction().commit();
    }

    @Test
    public void testParallelInsert() throws Exception {
        final ExecutorService executorService = Executors.newFixedThreadPool(4);
        total = 200000;
        final int batch = 100;
        for(int i=0; i< total/batch; i++) {
            executorService.execute(()-> {
                final EntityManager entityManager = emf.createEntityManager();
                entityManager.getTransaction().begin();
                for (int j=0;j<batch;j++) {
                    final User user = new User();
                    user.setName("bali balo" + nanoTime());
                    final Biography biography = new Biography();
                    biography.setContent(loremIpsum + nanoTime() + loremIpsum + loremIpsum);
                    entityManager.persist(user);
                    entityManager.persist(biography);
                }
                entityManager.getTransaction().commit();
                entityManager.close();
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1, MINUTES);



    }

    @Test
    public void testQueryAllUser() throws Exception {
        final SqlCounter counter = new SqlCounter();
        ds.addListener(counter);

        for(int i = 0; i<3;i++){
            final TypedQuery<User> query = em.createQuery("select u from org.blep.jpa.InsertTest$User u",
                    User.class);
            query.setHint("org.hibernate.fetchSize",1000);

            final Stopwatch started = Stopwatch.createStarted();
            final List<User> all = query.getResultList();
            System.out.println("started.elapsed(MILLISECONDS) = " + started.elapsed(MILLISECONDS));;



            System.out.println("all.size() = " + all.size());
            System.out.println("listener.getCount() = " + counter.getCount());

            assertThat(counter.getCount()).isEqualTo(1);
            counter.reset();
        }

    }


    @Test
    public void testQueryAllBiography() throws Exception {
        final SqlCounter counter = new SqlCounter();
        ds.addListener(counter);

        final TypedQuery<Biography> query = em.createQuery("select b from org.blep.jpa.InsertTest$Biography b"
                , Biography.class);
        query.setHint("org.hibernate.fetchSize",1000);

        final Stopwatch started = Stopwatch.createStarted();
        final List<Biography> bios = query.getResultList();
        System.out.println("started.elapsed(MILLISECONDS) = " + started.elapsed(MILLISECONDS));;

        System.out.println("bios = " + bios.size());
        assertThat(bios).isNotEmpty();
        assertThat(counter.getCount()).isEqualTo(1);

    }
}
