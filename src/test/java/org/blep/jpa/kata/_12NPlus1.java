package org.blep.jpa.kata;

import com.google.common.base.Stopwatch;
import io.blep.spysql.SqlCounter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.blep.jpa.JpaBaseTest;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.*;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.blep.jpa.JpaBaseTest.ServerDataStore;
import static org.fest.assertions.Assertions.assertThat;

/**
 * User: blep
 * Date: 16/09/14
 * Time: 19:33
 */

public class _12NPlus1 extends JpaBaseTest implements ServerDataStore{


    private final String className = getClass().getName();
    private EntityManager em;
    private final int carsNb = 50;

    @Entity
    @Data
    @EqualsAndHashCode(exclude = "id")
    public static class CarManufacturer{
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private Long id;

        private String name;
    }

    @Entity
    @Data
    @EqualsAndHashCode(exclude = "id")
    public static class CarModel{
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private Long id;

        private String modelName;

        @ManyToOne(optional = false)
        private CarManufacturer manufacturer;
    }

    @Entity
    @Data
    @EqualsAndHashCode(exclude = "id")
    public static class Car{
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private Long id;

        private String serial;

        @ManyToOne(optional = false)
        private CarModel model;
    }

    @Test
    public void testSearchNPlus1() {
        final SqlCounter counter = new SqlCounter();
        ds.addListener(counter);

        final Stopwatch watch = Stopwatch.createStarted();

        final List<Car> cars = em.createQuery("from " + className + "$Car", Car.class)
                .getResultList();

        for (Car car : cars) {
            System.out.println(String.format("%s\t%s\t%s",
                    car.getSerial(),
                    car.getModel().getModelName(),
                    car.getModel().getManufacturer().getName()));
        }
        System.err.println("watch.elapsed(MILLISECONDS) = " + watch.elapsed(MILLISECONDS));

        assertThat(cars).hasSize(2*carsNb);

        System.out.println("counter.getCount() = " + counter.getCount());
        assertThat(counter.getCount()).isEqualTo(1);
    }

    @Test
    public void testSearchFetch() {
        System.out.println("_12NPlus1.testSearch");

        final SqlCounter counter = new SqlCounter();
        ds.addListener(counter);

        final String queryString = "select c from " + className + "$Car c"
                +" join fetch c.model cm join fetch cm.manufacturer ";

        final Stopwatch watch = Stopwatch.createStarted();
        final List<Car> cars = em.createQuery(queryString, Car.class)
                .getResultList();

        for (Car car : cars) {
            System.out.println(String.format("%s\t%s\t%s",
                    car.getSerial(),
                    car.getModel().getModelName(),
                    car.getModel().getManufacturer().getName()));
        }
        System.err.println("watch.elapsed(MILLISECONDS) = " + watch.elapsed(MILLISECONDS));

        assertThat(cars).hasSize(2*carsNb);
        System.out.println("counter.getCount() = " + counter.getCount());

        assertThat(counter.getCount()).isEqualTo(1);
    }



    @Before
    public void setUp() throws Exception {
        final CarManufacturer peugeot = new CarManufacturer();
        peugeot.setName("Peugeot");
        final CarModel p205 = new CarModel();
        p205.setModelName("205");
        p205.setManufacturer(peugeot);


        final CarManufacturer renault = new CarManufacturer();
        renault.setName("Renault");
        final CarModel r5 = new CarModel();
        r5.setModelName("5");
        r5.setManufacturer(renault);

        em = emf.createEntityManager();
        em.getTransaction().begin();

        em.persist(peugeot);
        em.persist(p205);

        em.persist(renault);
        em.persist(r5);

        for (int i = 0; i < carsNb; i++) {
            final Car aR5 = new Car();
            aR5.setModel(r5);
            aR5.setSerial("r5-" + System.nanoTime());
            em.persist(aR5);

            final Car a205 = new Car();
            a205.setModel(p205);
            a205.setSerial("205-" + System.nanoTime());
            em.persist(a205);
        }

        em.getTransaction().commit();
        em.clear();



    }

    @Override
    protected List<Class<?>> persistentClasses() {
        return Arrays.asList(CarManufacturer.class,
                CarModel.class, Car.class);
    }

    @Override
    protected Properties config() {
        final Properties config = super.config();
        config.put("hibernate.show_sql", true);
        config.put("hibernate.format_sql", true);
        return config;
    }
}
