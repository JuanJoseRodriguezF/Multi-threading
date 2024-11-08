package com.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

public class Ejemplo {
    private static final int NUM_PREP_STATIONS = 3; // Número de estaciones de preparación disponibles
    private static Semaphore prepStations = new Semaphore(NUM_PREP_STATIONS);
    private static CountDownLatch ordersReadyLatch;

    public static void main(String[] args) {
        List<Order> orders = Arrays.asList(
            new Order("Burger"),
            new Order("Pizza"),
            new Order("Sushi"),
            new Order("Salad"),
            new Order("Taco")
        );

        ordersReadyLatch = new CountDownLatch(orders.size());
        ExecutorService executor = Executors.newFixedThreadPool(orders.size());

        // Procesamiento de pedidos
        List<Future<String>> futures = new ArrayList<>();
        for (Order order : orders) {
            futures.add(executor.submit(new ProcessOrderTask(order)));
        }

        try {
            // Espera a que todos los pedidos estén listos
            ordersReadyLatch.await();
            System.out.println("Todos los pedidos están listos para ser entregados.");

            // Imprime el estado de cada pedido
            for (Future<String> future : futures) {
                System.out.println(future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    // Tarea de procesamiento de pedidos
    static class ProcessOrderTask implements Callable<String> {
        private final Order order;

        public ProcessOrderTask(Order order) {
            this.order = order;
        }

        @Override
        public String call() throws Exception {
            // Procesamiento del pago en paralelo
            Thread paymentThread = new Thread(new PaymentTask(order));
            paymentThread.start();
            paymentThread.join(); // Espera a que el pago termine

            // Preparación de la comida
            prepStations.acquire(); // Espera hasta que haya una estación de preparación disponible
            try {
                order.prepare();
            } finally {
                prepStations.release(); // Libera la estación de preparación
            }

            ordersReadyLatch.countDown(); // Indica que el pedido está listo
            return "Pedido de " + order.getItem() + " completado.";
        }
    }

    // Clase Pedido
    static class Order {
        private final String item;
        private boolean paid = false;

        public Order(String item) {
            this.item = item;
        }

        public synchronized void pay() {
            System.out.println("Procesando pago para " + item);
            try {
                Thread.sleep(500); // Simula el tiempo de pago
                paid = true;
                System.out.println("Pago completado para " + item);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public synchronized void prepare() {
            System.out.println("Preparando " + item);
            try {
                Thread.sleep(1000); // Simula el tiempo de preparación
                System.out.println("Preparación completada para " + item);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public String getItem() {
            return item;
        }

        public boolean isPaid() {
            return paid;
        }
    }

    // Tarea de procesamiento de pago
    static class PaymentTask implements Runnable {
        private final Order order;

        public PaymentTask(Order order) {
            this.order = order;
        }

        @Override
        public void run() {
            order.pay();
        }
    }
}