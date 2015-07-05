package funcset;

import java.util.ArrayList;

public class Vector {
    
    private ArrayList<String> symbols;
    
    public Vector(){
        this.symbols=new ArrayList<>();
    }
    
    public void add(String s){
        this.symbols.add(s);
    }

    public void setSymbols(ArrayList<String> symbols) {
        this.symbols = symbols;
    }

    public ArrayList<String> getSymbols() {
        return symbols;
    }
    
}
