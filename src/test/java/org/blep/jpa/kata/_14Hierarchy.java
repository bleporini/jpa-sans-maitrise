package org.blep.jpa.kata;

import com.google.common.base.Stopwatch;
import io.blep.spysql.SqlCounter;
import lombok.*;
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

public class _14Hierarchy extends JpaBaseTest implements ServerDataStore{


    private final String className = getClass().getName();
    private EntityManager em;
    private final int carsNb = 50;

    @Entity
    @Data
    @EqualsAndHashCode(exclude = "id")
    public static class Manufacturer {
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private Long id;

        private String name;
    }

    @Entity
    @Data
    @EqualsAndHashCode(exclude = {"id","manufacturer"})
    public static class Model {
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private Long id;

        private String modelName;

        @ManyToOne(optional = false)
        private Manufacturer manufacturer;
    }

    @Inheritance(strategy = InheritanceType.JOINED)
    @Entity
    @Data
    @EqualsAndHashCode(exclude = {"id", "model", "owner"})
    public static abstract class Vehicle {
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private Long id;

        private String serial;

        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        private Model model;

        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        private Owner owner;
    }

    @Entity
    public static abstract class Aircraft extends Vehicle{

    }

    @Entity
    public static class Plane extends Aircraft{

    }

    @Entity
    public static class Helicopter extends Aircraft{

    }

    @Entity
    public static class LandVehicle extends Vehicle{

    }

    @Entity
    @Data @EqualsAndHashCode
    public static class Car extends LandVehicle{
        private Integer horsePowers;
    }

    @Entity
    @Data @EqualsAndHashCode
    public static class Bicycle extends LandVehicle{
    }
    @Entity
    @Data @EqualsAndHashCode
    public static class MotorBike extends LandVehicle{
        private Integer horsePowers;
    }

    @Entity
    public static abstract class Boat extends Vehicle{

    }

    @Entity
    public static class JetSki extends Boat{

    }





    @Entity
    @Data
    @EqualsAndHashCode(exclude = "id")
    public static class Owner {
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private Long id;

        private String name;

    }



    @Test
    public void testSearchPolymorph() {
        System.out.println("_12NPlus1.testSearch");

        final SqlCounter counter = new SqlCounter();
        ds.addListener(counter);

        final String queryString = "select c from " + className + "$Vehicle c order by c.serial"
          ;

        final Stopwatch watch = Stopwatch.createStarted();
        final List<Vehicle> cars = em.createQuery(queryString, Vehicle.class)
                .setMaxResults(10)
                .getResultList();

        for (Vehicle vehicle : cars) {
            System.out.println(String.format("%s\t%s",
                    vehicle.getId(),
                    vehicle.getSerial()));
        }
        System.err.println("watch.elapsed(MILLISECONDS) = " + watch.elapsed(MILLISECONDS));

//        assertThat(cars).hasSize(2*carsNb);
        System.out.println("counter.getCount() = " + counter.getCount());

        assertThat(counter.getCount()).isEqualTo(1);
    }

    @AllArgsConstructor
    @Getter @Setter
    public static class VehicleDto{
        private Long vehicleId;
        private String serial;

        public VehicleDto(Vehicle v) {
            this.vehicleId = v.getId();
            this.serial = v.getSerial();
        }
    }

    @Test
    public void testSearchDto() {
        System.out.println("_12NPlus1.testSearch");

        final SqlCounter counter = new SqlCounter();
        ds.addListener(counter);

        final String queryString = "select new "+className +"$VehicleDto(c.id, c.serial)" +
                " from " + className + "$Vehicle c order by c.serial"
                ;

        System.err.println("queryString = " + queryString);
        final Stopwatch watch = Stopwatch.createStarted();
        final List<VehicleDto> cars = em.createQuery(queryString, VehicleDto.class)
                .setMaxResults(10)
                .getResultList();

        for (VehicleDto dto : cars) {
            System.out.println(String.format("%s\t%s",
                    dto.getVehicleId(),
                    dto.getSerial()));
        }
        System.err.println("watch.elapsed(MILLISECONDS) = " + watch.elapsed(MILLISECONDS));

//        assertThat(cars).hasSize(2*carsNb);
        System.out.println("counter.getCount() = " + counter.getCount());

        assertThat(counter.getCount()).isEqualTo(1);
    }


    @Test
    public void testSearchDtoShallowQuery() {
        System.out.println("_12NPlus1.testSearch");

        final SqlCounter counter = new SqlCounter();
        ds.addListener(counter);

        final String queryString = "select new "+className +"$VehicleDto(c)" +
                " from " + className + "$Vehicle c order by c.serial"
                ;

        final Stopwatch watch = Stopwatch.createStarted();
        final List<VehicleDto> cars = em.createQuery(queryString, VehicleDto.class)
                .setMaxResults(10)
                .getResultList();

        for (VehicleDto car : cars) {
            System.out.println(String.format("%s\t",
                    car.getSerial()));
        }
        System.err.println("watch.elapsed(MILLISECONDS) = " + watch.elapsed(MILLISECONDS));

//        assertThat(cars).hasSize(2*carsNb);
        System.out.println("counter.getCount() = " + counter.getCount());

        assertThat(counter.getCount()).isEqualTo(1);
    }


    @Before
    public void setUp() throws Exception {
        final Manufacturer peugeot = new Manufacturer();
        peugeot.setName("Peugeot");
        final Model p205 = new Model();
        p205.setModelName("205");
        p205.setManufacturer(peugeot);


        final Manufacturer renault = new Manufacturer();
        renault.setName("Renault");
        final Model r5 = new Model();
        r5.setModelName("5");
        r5.setManufacturer(renault);

        em = emf.createEntityManager();
        em.getTransaction().begin();

        em.persist(peugeot);
        em.persist(p205);

        em.persist(renault);
        em.persist(r5);

        int ownerNb = 0;

        for (int i = 0; i < carsNb; i++) {
            final Owner owner1 = new Owner();
            owner1.setName("owner"+ownerNb++);
            em.persist(owner1);

            final Vehicle aR5 = new Car();
            aR5.setModel(r5);
            aR5.setSerial("r5-" + System.nanoTime());
            aR5.setOwner(owner1);
            em.persist(aR5);

            final Owner owner2 = new Owner();
            owner2.setName("owner" + ownerNb++);
            em.persist(owner2);

            final Vehicle a205 = new Car();
            a205.setModel(p205);
            a205.setSerial("205-" + System.nanoTime());
            a205.setOwner(owner2);
            em.persist(a205);
            if(i%100==0){
                em.flush();
                em.clear();
            }
        }

        em.getTransaction().commit();
        em.clear();



    }

    @Override
    protected List<Class<?>> persistentClasses() {
        return Arrays.asList(Manufacturer.class,
                Model.class, Vehicle.class, Owner.class,Car.class,
                Aircraft.class, Boat.class, JetSki.class,
                Helicopter.class, Plane.class);
    }

    @Override
    protected Properties config() {
        final Properties config = super.config();
        config.put("hibernate.show_sql", true);
        config.put("hibernate.format_sql", true);
        return config;
    }
}
