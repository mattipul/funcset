package funcset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class SETDatabase {

    private HashMap<String, Set> sets;
    private HashMap<String, Vector> vectors;
    private HashMap<String, String> variables;
    private HashMap<String, Function> functions;
    private ArrayList<String> rollbacks;

    public SETDatabase() {
        this.sets = new HashMap<>();
        this.variables = new HashMap<>();
        this.functions = new HashMap<>();
        this.vectors = new HashMap<>();
        this.rollbacks = new ArrayList<>();
    }

    public void emptyRollbacks() {
        this.rollbacks = new ArrayList<>();
    }

    public void rollback() {
        for (String r : rollbacks) {
            //System.out.println("Removing: "+r);
            sets.remove(r);
            variables.remove(r);
            vectors.remove(r);
            functions.remove(r);
        }
    }

    public int toValueInt(String e) {
        //System.out.println("ToValueInt(): " + e);
        if (e != null) {
            String ee = e.trim();
            if (ee.matches("-?\\d+")) {
                //System.out.println("Numerical");
                return Integer.parseInt(ee);
            } else {
                //System.out.println(ee);
                return toValueInt(getVar(ee));
            }
        }
        return -1;
    }

    public boolean toValueBoolean(String e) {
        if (e != null) {
            String ee = e.trim();
            //System.out.println(ee);
            if (ee.matches("(true|false)")) {
                return Boolean.valueOf(ee);
            } else {
                return toValueBoolean(getVar(ee));
            }
        }
        return false;
    }

    public void output(Function f) {
        System.out.println(f.getDomain() + " -> " + f.getCodomain());
        if(!f.getDefinitions().isEmpty()){
            for(String def:f.getDefinitions().keySet()){
                if(!def.equals("otherwise")){
                    System.out.println("\t"+f.getDefinition()+"="+f.getDefinitions().get(def)+", when "+def);
                }else{
                    if(f.getDefinitions().size()==1){
                        System.out.println("\t"+f.getDefinition()+"="+f.getDefinitions().get(def));
                    }else{
                        System.out.println("\t"+f.getDefinition()+"="+f.getDefinitions().get(def)+", otherwise");
                    }
                }
            }
        }
    }

    public String setToString(Set set, String h) {
        String ret = "";
        for (String s : set.getSymbols()) {
            if (getType(s) == 1) {
                ret += setToString(getSet(s), h + " ") + "\n";
            }
            if (getType(s) == 2) {
                ret += h + getVar(s) + "\n";
            }
            if (getType(s) == 3) {
                ret += h + getFunction(s) + "\n";
            }
        }
        System.out.println(ret);
        return ret;
    }

    public void output(Set set, String h) {
        String setStr = "";
        for (String s : set.getSymbols()) {
            if (getType(s) == 1) {
                output(getSetReal(s), h + " ");
            }
            if (getType(s) == 2) {
                Set sett = getSetReal(s);
                Vector vector = getVectorReal(s);
                Function f = getFunctionReal(s);
                if (sett == null && vector == null && f == null) {
                    System.out.println(h+toValueInt(s));
                }
                if (sett != null) {
                    output(sett, h + " ");
                }
                if (vector != null) {
                   output(vector, h + " ");
                }
                if (f != null) {
                    System.out.println(h+f.getDesc());
                }
               
            }
            if (getType(s) == 3) {
                System.out.println(h + getFunction(s).getDesc());
            }
        }
    }

    public void output(Vector v, String h) {
        for (String s : v.getSymbols()) {
            if (getType(s) == 1) {
                output(getSet(s), h + " ");
            }
            if (getType(s) == 2) {
                System.out.println(h + getVar(s));
            }
            if (getType(s) == 3) {
                System.out.println(h + getFunction(s));
            }
            if (getType(s) == 4) {
                output(getVector(s), h + " ");
            }
        }
    }

    public boolean exists(String k) {
        if (sets.containsKey(k)) {
            return true;
        }
        if (variables.containsKey(k)) {
            return true;
        }
        if (functions.containsKey(k)) {
            return true;
        }
        if (vectors.containsKey(k)) {
            return true;
        }
        return false;
    }

    public Set union(Set set1, Set set2) {
        Set s = new Set();
        ArrayList<String> symbols = new ArrayList<>();
        symbols.addAll(set1.getSymbols());
        symbols.addAll(set2.getSymbols());
        s.setSymbols(symbols);
        return s;
    }

    public Set not(Set set1, Set set2) {
        Set s = new Set();
        for (String e1 : set1.getSymbols()) {
            int i = 0;
            for (String e2 : set2.getSymbols()) {
                if (getType(e1) == 2 && getType(e2) == 2) {
                    if (getVar(e1).equals(getVar(e2))) {
                        i++;
                    }
                }
                if (getType(e1) == 1 && getType(e2) == 1) {
                    if (isSameSet(getSet(e1), getSet(e2))) {
                        i++;
                    }
                }
            }
            if (i == 0) {
                if (getType(e1) == 2) {
                    UUID uuid = UUID.randomUUID();
                    this.addVar(uuid.toString(), getVar(e1));
                    s.add(uuid.toString());
                }
                if (getType(e1) == 1) {
                    UUID uuid = UUID.randomUUID();
                    this.addSet(uuid.toString(), getSet(e1));
                    s.add(uuid.toString());
                }
            }
        }
        return s;
    }

    public Set intersection(Set set1, Set set2) {
        Set s = new Set();
        for (String e1 : set1.getSymbols()) {
            for (String e2 : set2.getSymbols()) {
                if (getType(e1) == 2 && getType(e2) == 2) {
                    if (getVar(e1).equals(getVar(e2))) {
                        UUID uuid = UUID.randomUUID();
                        this.addVar(uuid.toString(), getVar(e1));
                        s.add(uuid.toString());
                    }
                }
                if (getType(e1) == 1 && getType(e2) == 1) {
                    if (isSameSet(getSet(e1), getSet(e2)) && isSameSet(getSet(e2), getSet(e1))) {
                        UUID uuid = UUID.randomUUID();
                        this.addSet(uuid.toString(), getSet(e1));
                        s.add(uuid.toString());
                    }
                }
            }
        }
        return s;
    }

    public boolean isSameSet(Set set1, Set set2) {
        int in = 0;
        for (String e1 : set1.getSymbols()) {
            for (String e2 : set2.getSymbols()) {
                if (getType(e1) == 2 && getType(e2) == 2) {
                    if (getVar(e1).equals(getVar(e2))) {
                        in++;
                    }
                }
                if (getType(e1) == 1 && getType(e2) == 1) {
                    if (isSameSet(getSet(e1), getSet(e2)) && isSameSet(getSet(e2), getSet(e1))) {
                        in++;
                    }
                }
            }
        }
        if (in == set1.getSymbols().size()) {
            return true;
        }
        return false;
    }

    public int getType(String k) {
        if (sets.containsKey(k)) {
            return 1;
        }
        if (variables.containsKey(k)) {
            return 2;
        }
        if (functions.containsKey(k)) {
            return 3;
        }
        if (vectors.containsKey(k)) {
            return 4;
        }
        return 0;
    }

    public Set getSet(String k) {
        String ff = k;
        int t = getType(ff);
        while (t == 2) {
            ff = getVar(ff);
            t = getType(ff);
        }
        return this.sets.get(ff);
    }

    public Function getFunction(String k) {
        return this.functions.get(k);
    }

    public Function getFunctionReal(String k) {
        String ff = k;
        int t = getType(ff);
        while (t == 2) {
            ff = getVar(ff);
            t = getType(ff);
        }
        return this.functions.get(ff);
    }

    public String getVar(String k) {
        return this.variables.get(k);
    }

    public Vector getVector(String k) {
        return this.vectors.get(k);
    }

    public Set getSetReal(String k) {
        String ff = k;
        int t = getType(ff);
        while (t == 2) {
            ff = getVar(ff);
            t = getType(ff);
        }
        return this.sets.get(ff);
    }

    public Vector getVectorReal(String k) {
        String ff = k;
        int t = getType(ff);
        while (t == 2) {
            ff = getVar(ff);
            t = getType(ff);
        }
        return this.vectors.get(ff);
    }

    //MUISTA POISTOT
    public String addSet(String k, Set s) {
        rollbacks.add(k);
        this.sets.put(k, s);
        this.variables.remove(k);
        this.functions.remove(k);
        this.vectors.remove(k);
        return k;
    }

    //MUISTA POISTOT
    public String addVar(String k, String v) {
        rollbacks.add(k);
        this.variables.put(k, v);
        this.sets.remove(k);
        this.functions.remove(k);
        this.vectors.remove(k);
        return k;
    }

    //MUISTA POISTOT
    public String addFunction(String k, Function f) {
        rollbacks.add(k);
        this.functions.put(k, f);
        this.sets.remove(k);
        this.variables.remove(k);
        this.vectors.remove(k);
        return k;
    }

    //MUISTA POISTOT    
    public String addVector(String k, Vector v) {
        rollbacks.add(k);
        this.vectors.put(k, v);
        this.sets.remove(k);
        this.functions.remove(k);
        this.variables.remove(k);
        return k;
    }

    public HashMap<String, Set> getSets() {
        return sets;
    }

    public HashMap<String, String> getVariables() {
        return variables;
    }

    public HashMap<String, Vector> getVectors() {
        return vectors;
    }

    public HashMap<String, Function> getFunctions() {
        return functions;
    }

}
