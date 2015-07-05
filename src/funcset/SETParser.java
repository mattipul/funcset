package funcset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SETParser {

    interface IStdFunction {

        public String eval(String[] params);
    }

    class FUnion implements IStdFunction {

        @Override
        public String eval(String[] params) {
            if (params.length == 2) {
                String p1 = params[0];
                String p2 = params[1];
                if (p1 != null && p2 != null) {
                    UUID u1 = UUID.randomUUID();
                    UUID u2 = UUID.randomUUID();
                    evalStatement(u1.toString(), inside(p1));
                    evalStatement(u2.toString(), inside(p2));
                    Set s1 = database.getSetReal(u1.toString());
                    Set s2 = database.getSetReal(u2.toString());
                    if (s1 != null && s2 != null) {
                        Set sR = database.union(s1, s2);
                        UUID u = UUID.randomUUID();
                        database.addSet(u.toString(), sR);
                        return u.toString();
                    }
                }
            }
            return null;
        }

    }

    class FIntersection implements IStdFunction {

        @Override
        public String eval(String[] params) {
            if (params.length == 2) {
                String p1 = params[0];
                String p2 = params[1];
                if (p1 != null && p2 != null) {
                    UUID u1 = UUID.randomUUID();
                    UUID u2 = UUID.randomUUID();
                    evalStatement(u1.toString(), inside(p1));
                    evalStatement(u2.toString(), inside(p2));
                    Set s1 = database.getSetReal(u1.toString());
                    Set s2 = database.getSetReal(u2.toString());
                    if (s1 != null && s2 != null) {
                        Set sR = database.intersection(s1, s2);
                        UUID u = UUID.randomUUID();
                        database.addSet(u.toString(), sR);
                        return u.toString();
                    }
                }
            }
            return null;
        }

    }

    private SETDatabase database;
    private HashMap<String, IStdFunction> stdLib;
    private boolean hasErrors;
    private ArrayList<String> errors;

    public SETParser() {
        this.database = new SETDatabase();
        this.stdLib = new HashMap<>();
        this.errors = new ArrayList<>();
        initFunctions();
    }

    private void initFunctions() {
        this.stdLib.put("union", new FUnion());
        this.stdLib.put("intersection", new FIntersection());
    }

    private void addError(String error) {
        errors.add(error);
    }

    public Iterable<MatchResult> allMatches(
            final Pattern p, final CharSequence input) {
        return new Iterable<MatchResult>() {
            public Iterator<MatchResult> iterator() {
                return new Iterator<MatchResult>() {
                    // Use a matcher internally.
                    final Matcher matcher = p.matcher(input);
                    // Keep a match around that supports any interleaving of hasNext/next calls.
                    MatchResult pending;

                    public boolean hasNext() {
                        // Lazily fill pending, and avoid calling find() multiple times if the
                        // clients call hasNext() repeatedly before sampling via next().
                        if (pending == null && matcher.find()) {
                            pending = matcher.toMatchResult();
                        }
                        return pending != null;
                    }

                    public MatchResult next() {
                        // Fill pending if necessary (as when clients call next() without
                        // checking hasNext()), throw if not possible.
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        // Consume pending so next call to hasNext() does a find().
                        MatchResult next = pending;
                        pending = null;
                        return next;
                    }

                    /**
                     * Required to satisfy the interface, but unsupported.
                     */
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    private String[] splitWithDelimiters(String str, String regex) {
        List<String> parts = new ArrayList<String>();

        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(str);

        int lastEnd = 0;
        while (m.find()) {
            int start = m.start();
            if (lastEnd != start) {
                String nonDelim = str.substring(lastEnd, start);
                parts.add(nonDelim);
            }
            String delim = m.group();
            parts.add(delim);

            int end = m.end();
            lastEnd = end;
        }

        if (lastEnd != str.length()) {
            String nonDelim = str.substring(lastEnd);
            parts.add(nonDelim);
        }

        String[] res = parts.toArray(new String[]{});
        ////System.out.println("result: " + Arrays.toString(res));

        return res;
    }

    private String inside(String input) {
        return "(" + input + ")";
    }

    private int statementType(String input) {
        Pattern pattern1 = Pattern.compile("[#%\\\\]+");
        Matcher matcher1 = pattern1.matcher(input);

        Pattern pattern2 = Pattern.compile("(\\+|\\*|/|--)+");
        Matcher matcher2 = pattern2.matcher(input);

        Pattern pattern3 = Pattern.compile("[\\|=!<>&]+");
        Matcher matcher3 = pattern3.matcher(input);

        boolean b1 = matcher1.find();
        boolean b2 = matcher2.find();
        boolean b3 = matcher3.find();

        if (b1 && !b2 && !b3) {
            return 1;
        }
        if (!b1 && b2 && !b3) {
            return 2;
        }
        if (!b1 && !b2 && b3) {
            return 3;
        }
        return -1;
    }

    public int singleStatementType(String input) {
        if (input.matches("^[{]{1}.*[}]{1}$")) {
            return 1;
        } else if (input.matches("^[a-zA-Z]+[0-9]*[\\[]{1}[^\\[\\]]+[\\]]{1}$")) {
            return 2;
        } else if (input.contains(",")) {
            return 4;
        } else {
            return 3;
        }
    }

    private void evalStatement(String key, String input) {
        //////System.out.println("Database variables: "+this.database.getVariables().keySet().toString());
        ////System.out.println("Eval statement: " + input);
        for (MatchResult match : allMatches(Pattern.compile("[(]{1}[^()]+[)]{1}"), input)) {
            String statement = match.group();
            String statementNoBrackets = statement.substring(1, statement.length() - 1);
            //System.out.println("Substatement: " + statementNoBrackets);
            int s = match.start();
            int statementType = statementType(statementNoBrackets);
            //System.out.println("Statement type: " + statementType);
            UUID uuid = UUID.randomUUID();
            //System.out.println("To be saved: " + uuid.toString());
            if (statementType == 1) {
                //System.out.println("SET OPERATION");
                Set set = evalSetDef(statementNoBrackets);
                if (set == null) {
                    addError("Set operation returns null.");
                    hasErrors = true;
                }
                database.addSet(uuid.toString(), set);
            }
            if (statementType == 2) {
                //System.out.println("VALUE OPERATION");
                String value = this.evalValue(statementNoBrackets);
                if (value == null) {
                    addError("Value operation returns null.");
                    hasErrors = true;
                }
                database.addVar(uuid.toString(), value);
            }
            if (statementType == 3) {
                //System.out.println("BOOLEAN OPERATION");
                String bool = this.evalLogic(statementNoBrackets);
                if (bool == null) {
                    addError("Boolean operation returns null.");
                    hasErrors = true;
                }
                database.addVar(uuid.toString(), bool);
            }
            if (statementType == -1) {
                //System.out.println("SINGLE");
                int singleStatementType = singleStatementType(statementNoBrackets);
                if (singleStatementType == 1) {
                    ////System.out.println("SET");
                    UUID u = UUID.randomUUID();
                    parseSet(u.toString(), statementNoBrackets);
                    Set set = this.database.getSet(u.toString());
                    if (set == null) {
                        addError("Set construction FAILED.");
                        hasErrors = true;
                        ////System.out.println("Set is null");
                    }
                    database.addSet(uuid.toString(), set);
                }
                if (singleStatementType == 2) {
                    //System.out.println("FUNCTION VALUE");
                    String[] splitted = statementNoBrackets.split("[\\[]{1}");
                    String paramsStr = splitted[1].substring(0, splitted[1].length() - 1);
                    //////System.out.println(paramsStr);
                    String[] params = paramsStr.split(",");
                    if (params.length == 0) {
                        params = new String[]{paramsStr};
                    }
                    for (int i = 0; i < params.length; i++) {
                        params[i] = params[i];
                    }
                    String fvalue = this.functionValue(splitted[0], params);
                    //System.out.println("Function value: " + fvalue);
                    if (fvalue == null) {
                        addError("Function returns null.");
                        hasErrors = true;
                    }
                    this.database.addVar(uuid.toString(), fvalue);
                }
                if (singleStatementType == 3) {
                    //System.out.println("SYMBOL");
                    if (!statementNoBrackets.matches("-?\\d+")) {
                        if (!database.exists(statementNoBrackets)) {
                            addError("Statement: " + statement);
                            addError("Pointing to a non-existent symbol: " + statementNoBrackets);
                            hasErrors = true;
                        }
                    }
                    database.addVar(uuid.toString(), statementNoBrackets);
                }
                if (singleStatementType == 4) {
                    //System.out.println("VECTOR");
                    Vector v = constructVector(uuid.toString(), statementNoBrackets);
                    if (v == null) {
                        addError("Vector construction FAILED.");
                        hasErrors = true;
                    }
                    database.addVector(uuid.toString(), v);
                }
            }
            if (s + statement.length() < input.length()) {
                String newStatement = input.substring(0, s);
                newStatement += uuid + input.substring(s + statement.length(), input.length());
                evalStatement(key, newStatement);
            } else {
                evalStatement(key, uuid.toString());
            }
        }
        if (!input.matches("^[(]{1}.+[)]{1}$")) {

            if (input.matches("-?\\d+")) {
                int e = this.database.toValueInt(input);
                //System.out.println("Element is integer: " + e);
                database.addVar(key, "" + e);
            } else {
                int elementType = this.database.getType(input);
                if (elementType == 1) {
                    if (database.getSets().containsKey(input)) {
                        //System.out.println("Saving a set");
                        Set s = this.database.getSet(input);
                        if (s == null) {
                            //addError("Set construction FAILED.");
                            hasErrors = true;
                            ////System.out.println("Set is null");
                        }
                        database.addSet(key, s);
                    }
                }
                if (elementType == 2) {
                    if (database.getVariables().containsKey(input)) {
                        //System.out.println("Saving a variable: " + input);
                        database.addVar(key, input);
                    }
                }
                if (elementType == 3) {
                    if (database.getFunctions().containsKey(input)) {
                        //System.out.println("Saving a function: " + input);
                        Function f = database.getFunctions().get(input);
                        if (f == null) {
                            hasErrors = true;
                        }
                        database.addFunction(key, f);
                    }
                }
                if (elementType == 4) {
                    if (database.getVectors().containsKey(input)) {
                        //System.out.println("Saving a vector");
                        Vector v = this.database.getVector(input);
                        if (v == null) {
                            hasErrors = true;
                            ////System.out.println("Vector is null");
                        }
                        database.addVector(key, v);
                    }
                }

            }
        }
    }

    private Vector constructVector(String uuid, String input) {
        String nInput = input;
        String[] splitted = nInput.split(",");
        Vector mVectorT = new Vector();
        if (splitted.length == 0) {
            splitted = new String[]{nInput};
        }
        for (String s : splitted) {
            ////System.out.println("Vector element: " + s);
            if (s.matches("-?\\d+")) {
                UUID setUUID = UUID.randomUUID();
                this.database.addVar(setUUID.toString(), s);
                mVectorT.add(setUUID.toString());
            } else if (s.matches("[a-zA-Z]+[0-9]*[\\[]{1}[^()]+[\\]]{1}")) {
                UUID setUUID = UUID.randomUUID();
                this.database.addVar(setUUID.toString(), s);
                mVectorT.add(setUUID.toString());
            } else {
                mVectorT.add(s);
            }
        }
        return mVectorT;
    }

    private boolean isNumeric(String key) {
        if(database.exists(key)){
            Set set = this.database.getSetReal(key);
            Vector vector = this.database.getVectorReal(key);
            Function f = this.database.getFunctionReal(key);
            if (set == null && vector == null && f == null) {
                return true;
            }
        }
        return false;
    }

    private boolean toBoolean(String input) {
        String[] splitted = this.splitWithDelimiters(input, "(==|!=|<=|>=|<|>)");
        if (splitted.length == 3) {
            //EVALSTATEMENT!
            if(!isNumeric(splitted[0]) || !isNumeric(splitted[1])){
                addError("Boolean operation accepts numeric values only.");
                hasErrors=true;
            }
            int e1 = this.database.toValueInt(splitted[0]);
            int e2 = this.database.toValueInt(splitted[2]);
            String d = splitted[1];
            if (d.equals("==")) {
                return e1 == e2;
            }
            if (d.equals("!=")) {
                return e1 != e2;
            }
            if (d.equals("<=")) {
                return e1 <= e2;
            }
            if (d.equals(">=")) {
                return e1 >= e2;
            }
            if (d.equals("<")) {
                return e1 < e2;
            }
            if (d.equals(">")) {
                return e1 > e2;
            }
        }
        if (splitted.length == 1) {
            return this.database.toValueBoolean(input);
        }
        return false;
    }

    private String evalLogic(String input) {
        String[] splitted = this.splitWithDelimiters(input, "(&&|\\|\\|)");
        if (splitted.length > 1) {
            boolean tal = toBoolean(String.valueOf(splitted[0]));
            for (int i = 1; i < splitted.length; i += 2) {
                if (splitted[i].equals("&&")) {
                    tal = toBoolean(String.valueOf(tal)) && toBoolean(splitted[i + 1]);
                } else if (splitted[i].equals("||")) {
                    tal = toBoolean(String.valueOf(tal)) || toBoolean(splitted[i + 1]);
                }
            }
            return String.valueOf(tal);
        } else {
            return String.valueOf(toBoolean("" + splitted[0]));
        }
    }

    private String evalValue(String input) {
        String[] splitted = this.splitWithDelimiters(input, "(\\+|\\*|/|--)");
        if (splitted.length > 2) {
            int tal = 0;
            if (splitted[0].matches("-?\\d+")) {
                tal = this.database.toValueInt(splitted[0]);
            } else {
                //System.out.println("Exists: " + database.exists(splitted[0]));
                if (database.exists(splitted[0])) {
                    UUID u = UUID.randomUUID();
                    evalStatement(u.toString(), inside(splitted[0]));
                    tal = this.database.toValueInt(u.toString());
                } else {
                    return null;
                }
            }
            for (int i = 1; i < splitted.length; i += 2) {
                int e = 0;
                if (splitted[i + 1].matches("-?\\d+")) {
                    e = this.database.toValueInt(splitted[i + 1]);
                } else {
                    if (database.exists(splitted[i + 1])) {
                        UUID u = UUID.randomUUID();
                        evalStatement(u.toString(), inside(splitted[i + 1]));
                        e = this.database.toValueInt(u.toString());
                    } else {
                        return null;
                    }
                }
                if (splitted[i].equals("+")) {
                    tal = tal + e;
                }
                if (splitted[i].equals("--")) {
                    tal = tal - e;
                }
                if (splitted[i].equals("*")) {
                    tal = tal * e;
                }
                if (splitted[i].equals("/")) {
                    tal = tal / e;
                }
            }
            ////System.out.println("Value operation result: " + tal);
            return "" + tal;
        } else if (splitted.length == 1) {
            return "" + this.database.toValueInt(input);
        }
        return null;
    }

    private void parseSet(String uuid, String input) {
        for (MatchResult match : allMatches(Pattern.compile("[{]{1}[^{}]+[}]{1}"), input)) {
            String set = match.group();
            String setNoBrackets = set.substring(1, set.length() - 1);
            int s = match.start();
            Set mSet = this.constructSet(setNoBrackets);
            UUID setUUID = UUID.randomUUID();
            database.addSet(setUUID.toString(), mSet);
            if (s + set.length() < input.length()) {
                String newSet = input.substring(0, s);
                newSet += setUUID + input.substring(s + set.length(), input.length());
                parseSet(uuid, newSet);
            } else {
                parseSet(uuid, setUUID.toString());
            }
        }
        if (!input.matches("^[{]{1}.+[}]{1}$")) {
            HashMap<String, Set> sets = database.getSets();
            if (sets.containsKey(input)) {
                database.addSet(uuid, sets.get(input));
            }
        }
    }

    private Set constructSet(String input) {
        String nInput = input;
        String[] splitted = nInput.split(",");
        Set mSetT = new Set();
        if (splitted.length == 0) {
            splitted = new String[]{nInput};
        }
        for (String s : splitted) {
            ////System.out.println("Set element: " + s);
            if (s.matches("-?\\d+")) {
                UUID setUUID = UUID.randomUUID();
                this.database.addVar(setUUID.toString(), s);
                mSetT.add(setUUID.toString());
            } else if (s.matches("[a-zA-Z]+[0-9]*[\\[]{1}[^()]+[\\]]{1}")) {
                UUID setUUID = UUID.randomUUID();
                this.database.addVar(setUUID.toString(), s);
                mSetT.add(setUUID.toString());
            } else {
                mSetT.add(s);
            }
        }
        return mSetT;
    }

    private Set evalSetDef(String input) {
        String[] splitted = this.splitWithDelimiters(input, "[#%\\\\]{1}");
        if (splitted.length > 1) {
            UUID setUUID = UUID.randomUUID();
            evalStatement(setUUID.toString(), inside(splitted[0]));
            Set tal = this.database.getSet(setUUID.toString());
            for (int i = 1; i < splitted.length; i += 2) {
                UUID talUUID = UUID.randomUUID();
                if (splitted[i].equals("#")) {
                    evalStatement(talUUID.toString(), inside(splitted[i + 1]));
                    Set t = this.database.getSet(talUUID.toString());
                    tal = database.intersection(tal, t);
                } else if (splitted[i].equals("%")) {
                    evalStatement(talUUID.toString(), inside(splitted[i + 1]));
                    Set t = this.database.getSet(talUUID.toString());
                    tal = database.union(tal, t);
                } else if (splitted[i].equals("\\")) {
                    evalStatement(talUUID.toString(), inside(splitted[i + 1]));
                    Set t = this.database.getSet(talUUID.toString());
                    tal = database.not(tal, t);
                }
            }
            return tal;
        } else {
            ////System.out.println("EvalSetDef(): No operations");
            UUID setUUID = UUID.randomUUID();
            evalStatement(setUUID.toString(), inside(input));
            return database.getSets().get(setUUID.toString());
        }
    }

    public boolean isDefinition(String input) {
        if (input.matches("^[a-zA-Z]+[0-9]*[=]{1}.+$")) {
            return true;
        }
        return false;
    }

    private boolean isFunction(String input) {
        if (input.matches("^[a-zA-Z]+[:]{1}.+(->).+")) {
            return true;
        }
        return false;
    }

    public boolean isFunctionDefinition(String input) {
        if (input.matches("^[a-zA-Z]+[0-9]*[(]{1}[a-zA-Z0-9,]+[)]{1}[=]{1}(.+[\\r\\n]*)+$")) {
            return true;
        }
        return false;
    }

    public String functionValue(String f, String[] params) {
        //System.out.println("Evaluating function: " + f+" "+params.length);
        if (this.stdLib.containsKey(f)) {
            String ret = this.stdLib.get(f).eval(params);
            ////System.out.println("Using standard library: " + f);
            return ret;
        }
        int ci = 0;
        for (String p : params) {
            UUID u = UUID.randomUUID();
            evalStatement(u.toString(), "(" + p + ")");
            params[ci] = u.toString();
            ci++;
        }
        Function func = this.database.getFunction(f);
        if (func.getDomain().equals("Integer")) {
            if (params.length != 1) {
                addError("Function domain is [Integer] not [Vector].");
                hasErrors = true;
                return null;

            } else if (params.length == 1) {
                Set s = database.getSetReal(params[0]);
                if (s != null) {
                    addError("Function domain is [Integer] not [Set].");
                    hasErrors = true;
                    return null;
                }
            }
        }
        if (func.getDomain().equals("Vector")) {
            if (params.length < 2) {
                addError("Function domain is [Vector] not [Integer] or [Set].");
                hasErrors = true;
                return null;
            }
        }
        if (func.getDomain().equals("Set")) {
            if (params.length != 1) {
                addError("Function domain is [Set] not [Vector].");
                hasErrors = true;
                return null;
            } else if (params.length == 1) {
                for (String p : params) {
                    Set s = database.getSetReal(p);
                    if (s == null) {
                        addError("Function domain is [Set] not [Vector] or [Integer].");
                        hasErrors = true;
                        return null;
                    }
                }
            }
        }
        if (func != null) {
            java.util.Set<String> str = func.getDefinitions().keySet();
            String correctDef = "";
            for (String s : str) {
                if (!s.equals("otherwise")) {
                    String d = s;
                    int c = 0;
                    //System.out.println(s);
                    for (String j : func.getParameters()) {
                        UUID u1 = UUID.randomUUID();
                        //System.out.println("Function parameter " + c + ": " + params[c]);
                        this.database.addVar(u1.toString(), params[c]);
                        ////System.out.println("Testing database variable: " + this.database.toValueInt(u1.toString()));
                        d = d.replaceAll("(" + j + "){1}", u1.toString());
                        c++;
                    }
                    UUID u = UUID.randomUUID();
                    //System.out.println(d);
                    evalStatement(u.toString(), "(" + d + ")");
                    //ONGELMA?
                    String b = u.toString();
                    //System.out.println("Evaluated boolean: " + database.toValueBoolean(b));
                    if (database.toValueBoolean(b) == true) {
                        correctDef = func.getDefinitions().get(s);
                    }
                }
            }
            if (correctDef.length() > 0) {
                int c = 0;
                for (String j : func.getParameters()) {
                    UUID u1 = UUID.randomUUID();
                    this.database.addVar(u1.toString(), params[c]);
                    correctDef = correctDef.replaceAll("(" + j + "){1}", u1.toString());
                    c++;
                }
                UUID u = UUID.randomUUID();
                evalStatement(u.toString(), "(" + correctDef + ")");
                String b = u.toString();
                return b;
            } else {
                correctDef = func.getDefinitions().get("otherwise");
                int c = 0;
                for (String j : func.getParameters()) {
                    //System.out.println("Parameter: " + j);
                    UUID u1 = UUID.randomUUID();
                    this.database.addVar(u1.toString(), params[c]);
                    correctDef = correctDef.replaceAll("(" + j + "){1}", u1.toString());
                    c++;
                }
                UUID u = UUID.randomUUID();
                evalStatement(u.toString(), "(" + correctDef + ")");
                String b = u.toString();
                //System.out.println(b);
                return b;
            }
        } else {
            Vector v = database.getVectorReal(f);
            if (v != null) {
                ////System.out.println("Getting an element of a vector");
                if (params.length == 1) {
                    String element = v.getSymbols().get(Integer.parseInt(params[0]));
                    ////System.out.println("Vector element: " + element);
                    return element;
                }
            }
        }
        return null;
    }

    public void testF() {
        ////System.out.println(this.database.toValueInt(functionValue("f", new String[]{"1", "2"})));
    }

    private void printErrors() {
        for (String e : errors) {
            System.out.println("ERROR: " + e);
        }
    }

    public void printDatabase() {
        System.out.println("\nDATABASE");
        System.out.println("VARIABLES:");
        for (String k : this.database.getVariables().keySet()) {
            System.out.println(k + " : " + this.database.getVariables().get(k));
        }
        System.out.println("VECTORS:");
        for (String k : this.database.getVectors().keySet()) {
            System.out.println(k + ":");
            this.database.output(this.database.getVectors().get(k), "");
        }
        System.out.println("SETS:");
        for (String k : this.database.getSets().keySet()) {
            System.out.println(k + ":");
            this.database.output(this.database.getSets().get(k), "");
        }
    }

    public boolean eval(String input) {
        ////System.out.println("-: " + input);
        hasErrors = false;
        errors = new ArrayList<>();
        database.emptyRollbacks();
        if (input.startsWith("?- ")) {
            String statement = inside(input.substring(3));
            UUID u = UUID.randomUUID();
            evalStatement(u.toString(), statement);

            //System.out.println(u.toString());
            //printDatabase();
            if (!hasErrors) {
                if (database.exists(u.toString())) {
                    Set set = this.database.getSetReal(u.toString());
                    Vector vector = this.database.getVectorReal(u.toString());
                    Function f = this.database.getFunctionReal(u.toString());
                    if (set == null && vector == null && f == null) {
                        System.out.println(this.database.toValueInt(u.toString()));
                    }
                    if (set != null) {
                        this.database.output(set, "");
                    }
                    if (vector != null) {
                        this.database.output(vector, "");
                    }
                    if (f != null) {
                        this.database.output(f);
                    }
                    return true;
                }
                return true;
            } else {
                this.printErrors();
            }
            return false;
        }
        if (isDefinition(input)) {
            ////System.out.println("IsDefinition=true");
            String[] splitted = input.replaceAll("-", "--").split("=");
            String statement = inside(splitted[1]);
            evalStatement(splitted[0], statement);
            int type = this.database.getType(splitted[0]);
            ////System.out.println("Definition type: " + type);

            if (hasErrors) {
                this.printErrors();
                this.database.rollback();
                return false;
            }
            return true;
        }
        if (isFunction(input)) {
            ////System.out.println("IsFunction=true");
            String[] splitted = input.split(":");
            String[] sets = splitted[1].split("->");
            /*UUID uuid1 = UUID.randomUUID();
             UUID uuid2 = UUID.randomUUID();
             evalStatement(uuid1.toString(), inside(sets[0]));
             evalStatement(uuid2.toString(), inside(sets[1]));*/
            if (!sets[0].matches("(Integer|Set|Vector)") || !sets[1].matches("(Integer|Set|Vector)")) {
                return false;
            }
            Function f = new Function(sets[0], sets[1]);
            f.setDesc(input);
            this.database.addFunction(splitted[0], f);
            // this.database.output(this.database.getSet(f.getDomain()), "");
            return true;
        }
        if (isFunctionDefinition(input)) {
            ////System.out.println("IsFunctionDefinition=true");
            String[] splittedDef = input.replaceAll("-", "--").split("=", 2);
            String[] splittedSymbol = splittedDef[0].split("[(]{1}");
            String parameters = splittedSymbol[1].substring(0, splittedSymbol[1].length() - 1);
            Function f = this.database.getFunction(splittedSymbol[0]);

            if (f != null) {
                f.setDefinition(splittedDef[0]);
                f.setParameters(Arrays.asList(parameters.split(",")));
                f.setDefinitions(new HashMap<String, String>());
                ////System.out.println("Function parameter count: " + f.getParameters().size());
                String[] defs = splittedDef[1].split("[|]");
                if (defs.length > 1) {
                    for (String def : defs) {
                        ////System.out.println(":" + def);
                        String[] defSingle = def.split("[;]");
                        if (defSingle.length == 2) {
                            f.addDefinition(defSingle[1], defSingle[0]);
                        } else {
                            f.addDefinition("otherwise", def);
                        }
                    }
                } else {
                    String[] defSingle = splittedDef[1].split("[;]");
                    if (defSingle.length == 2) {
                        f.addDefinition(defSingle[1], defSingle[0]);
                    } else {
                        f.addDefinition("otherwise", splittedDef[1]);
                    }
                }
            }

            return true;
        }
        return false;
    }

}
