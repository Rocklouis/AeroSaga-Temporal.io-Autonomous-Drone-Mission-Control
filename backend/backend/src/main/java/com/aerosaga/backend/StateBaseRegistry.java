
package com.aerosaga.backend;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class StateBaseRegistry {

    private static final Map<String, StateBase> BASES =
            new LinkedHashMap<>();

    static {
        add("Andhra Pradesh", "Amaravati", 16.5062, 80.6480);
        add("Arunachal Pradesh", "Itanagar", 27.0844, 93.6053);
        add("Assam", "Dispur", 26.1433, 91.7898);
        add("Bihar", "Patna", 25.5941, 85.1376);
        add("Chhattisgarh", "Raipur", 21.2514, 81.6296);
        add("Goa", "Panaji", 15.4909, 73.8278);
        add("Gujarat", "Gandhinagar", 23.2156, 72.6369);
        add("Haryana", "Chandigarh", 30.7333, 76.7794);
        add("Himachal Pradesh", "Shimla", 31.1048, 77.1734);
        add("Jharkhand", "Ranchi", 23.3441, 85.3096);
        add("Karnataka", "Bengaluru", 12.9716, 77.5946);
        add("Kerala", "Thiruvananthapuram", 8.5241, 76.9366);
        add("Madhya Pradesh", "Bhopal", 23.2599, 77.4126);
        add("Maharashtra", "Mumbai", 19.0760, 72.8777);
        add("Manipur", "Imphal", 24.8170, 93.9368);
        add("Meghalaya", "Shillong", 25.5788, 91.8933);
        add("Mizoram", "Aizawl", 23.7271, 92.7176);
        add("Nagaland", "Kohima", 25.6751, 94.1086);
        add("Odisha", "Bhubaneswar", 20.2961, 85.8245);
        add("Punjab", "Chandigarh", 30.7338, 76.7800);
        add("Rajasthan", "Jaipur", 26.9124, 75.7873);
        add("Sikkim", "Gangtok", 27.3389, 88.6065);
        add("Tamil Nadu", "Chennai", 13.0827, 80.2707);
        add("Telangana", "Hyderabad", 17.3850, 78.4867);
        add("Tripura", "Agartala", 23.8315, 91.2868);
        add("Uttar Pradesh", "Lucknow", 26.8467, 80.9462);
        add("Uttarakhand", "Dehradun", 30.3165, 78.0322);
        add("West Bengal", "Kolkata", 22.5726, 88.3639);
    }

    private static void add(
            String state,
            String capital,
            double latitude,
            double longitude) {

        BASES.put(
                state,
                new StateBase(
                        state,
                        capital,
                        latitude,
                        longitude
                )
        );
    }

    public static StateBase getBase(String stateName) {
        return BASES.get(stateName);
    }

    public static Map<String, StateBase> getAllBases() {
        return Collections.unmodifiableMap(BASES);
    }

    public static int getBaseCount() {
        return BASES.size();
    }
}
