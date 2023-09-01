package com.github.havlli.EventPilot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

public class PerformanceTest {
    private static final HashMap<Integer, String> fieldsMap = new HashMap<>(Map.of(
            -1, "Absence",
            -2, "Late",
            -3, "Tentative",
            1, "Tank",
            2, "Melee",
            3, "Ranged",
            4, "Healer",
            5, "Support"
    ));

    public static void main(String[] args) throws JsonProcessingException {
        // Sample HashMap<Integer, String>
        HashMap<Integer, String> hashMap = new HashMap<>();
        hashMap.put(1, "Group 1");
        hashMap.put(2, "Group 2");
        hashMap.put(3, "Group 3");
        hashMap.put(4, "Group 4");
        hashMap.put(5, "Group 5");
        hashMap.put(6, "Group 6");
        hashMap.put(7, "Group 7");
        hashMap.put(8, "Group 8");

        // Sample List<Entity>
        List<Entity> entityList = new ArrayList<>();
        entityList.add(new Entity(1, "Entity A"));
        entityList.add(new Entity(2, "Entity B"));
        entityList.add(new Entity(1, "Entity C"));
        entityList.add(new Entity(4, "Entity D"));
        entityList.add(new Entity(2, "Entity E"));

        ExecutionTimeTester.printExecutionTime("entityList to set then contains", () -> {
            Set<Integer> distinctIds = entityList.stream()
                    .map(entity -> entity.getKey())
                    .collect(Collectors.toSet());
            hashMap.entrySet()
                    .stream()
                    .filter(entry -> distinctIds.contains(entry.getKey()));
        });

        ExecutionTimeTester.printExecutionTime("grouping entities and then looping", () -> {
            Map<Integer, List<Entity>> groupedEntities = entityList.stream()
                    .collect(Collectors.groupingBy(Entity::getKey));
            for (Map.Entry<Integer, List<Entity>> entry : groupedEntities.entrySet()) {
                System.out.println("Group: " + entry.getKey());
                for (Entity entity : entry.getValue()) {
                    System.out.println("   " + entity);
                }
            }
        });

        ExecutionTimeTester.printExecutionTime("filter entityList.anyMatch", () -> {
            hashMap.entrySet()
                    .stream()
                    .filter(entry -> entityList.stream().anyMatch(entity -> entity.getKey() == entry.getKey()));
        });

        ExecutionTimeTester.printExecutionTime("looping over hashMap and check entityList.anyMatch", () -> {
            for (Map.Entry<Integer, String> entry : hashMap.entrySet()) {
                if (entityList.stream().anyMatch(entity -> entity.getKey() == entry.getKey())) {
                    System.out.println(entry);
                }
            }
        });

        ExecutionTimeTester.printExecutionTime("looping over hashMap and looping over entityList and then break on match", () -> {
            for (Map.Entry<Integer, String> entry : hashMap.entrySet()) {
                for (Entity entity : entityList) {
                    if (entity.getKey() == entry.getKey()) {
                        break;
                    }
                }
            }
        });

        ExecutionTimeTester.printExecutionTime("filter hashMap and looping over entityList and then break on match", () -> {
            hashMap.entrySet().stream()
                    .filter(entry -> {
                        for (Entity entity : entityList) {
                            if (entity.getKey() == entry.getKey())
                                return true;
                        }
                        return false;
                    });
        });

        ExecutionTimeTester.printExecutionTime("filter hashMap and anyMatch in entityList", () -> {
            hashMap.entrySet().stream()
                    .filter(entry -> entityList.stream()
                            .anyMatch(entity -> entity.getKey() == entry.getKey()));
        });

        ExecutionTimeTester.printExecutionTime("filter hashMap and filter findFirst in entityList", () -> {
            hashMap.entrySet().stream()
                    .filter(entry -> entityList.stream()
                            .filter(entity -> entity.getKey() == entry.getKey())
                            .findFirst()
                            .isPresent());
        });
    }

    public static String serializeMap() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(fieldsMap);
    }

    public static Map<Integer, String> deserializeMap(String json) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, HashMap.class);
    }
}

class Entity {
    private int key;
    private String value;

    public Entity(int key, String value) {
        this.key = key;
        this.value = value;
    }

    public int getKey() {
        return key;
    }

    @Override
    public String toString() {
        return "Entity{" +
                "key=" + key +
                ", value='" + value + '\'' +
                '}';
    }
}
