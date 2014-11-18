package org.blep.jpa.kata;

import com.google.common.base.Stopwatch;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.blep.jpa.JpaBaseTest;
import org.fest.assertions.Assertions;
import org.junit.After;
import org.junit.Test;

import javax.persistence.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.blep.jpa.JpaBaseTest.C3P0ServerDataStore;
import static org.fest.assertions.Assertions.assertThat;

/**
 * User: blep
 * Date: 16/09/14
 * Time: 08:38
 */

public class _11NoParameterBinding extends JpaBaseTest implements C3P0ServerDataStore{

    private final String className = this.getClass().getName();

    @Override
    protected List<Class<?>> persistentClasses() {
        return Arrays.asList(User.class);
    }

    @Entity
    @Data
    @EqualsAndHashCode(exclude = "id")
    public static class User {
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private Long id;

        private String name;
    }


    @Test
    public void parallel_queries() throws Exception {
        final ExecutorService service = Executors.newFixedThreadPool(getRuntime().availableProcessors());
        final Stopwatch watch = Stopwatch.createStarted();
        for (int i = 0; i < 10000; i++) {
            service.submit(() -> {
                final EntityManager em = emf.createEntityManager();
                final String queryString = "select u from "
                        + className + "$User u where u.id = " + System.nanoTime();
                final List resultList = em.createQuery(
                        queryString)
                        .getResultList();

                assertThat(resultList).isNotNull();
                assertThat(resultList).hasSize(0);
                em.close();
            });
        }

        service.shutdown();
        service.awaitTermination(1, HOURS);
        System.err.println("watch.elapsed(MILLISECONDS) = " + watch.elapsed(MILLISECONDS));
    }

    @Test
    public void parallel_queries_with_parameters() throws Exception {
        final ExecutorService service = Executors.newFixedThreadPool(4);

        final Stopwatch watch = Stopwatch.createStarted();
        for (int i = 0; i < 10000; i++) {
            service.submit(() -> {
                final EntityManager em = emf.createEntityManager();
                final String queryString = "select u from " + className
                        + "$User u where u.id = :id";
                final List resultList = em.createQuery(queryString)
                        .setParameter("id", System.nanoTime())
                        .getResultList();
                assertThat(resultList).isNotNull();
                assertThat(resultList).hasSize(0);
                em.close();
            });
        }

        service.shutdown();
        service.awaitTermination(1, HOURS);
        System.err.println("watch.elapsed(MILLISECONDS) = " + watch.elapsed(MILLISECONDS));
    }

    @After
    public void tearDown(){
        emf.close();
    }
}
