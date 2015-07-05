package funcset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Function {
    
    private String desc;
    private String domain;
    private String codomain;
    private String definition;
    private List<String> parameters;
    private HashMap<String, String> definitions;

    public Function(String domain, String codomain) {
        this.domain = domain;
        this.definitions=new HashMap<>();
        this.codomain = codomain;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public List<String> getParameters() {
        return parameters;
    }
    
    public void addDefinition(String k, String v){
        this.definitions.put(k, v);
    }

    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }

    public HashMap<String, String> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(HashMap<String, String> definitions) {
        this.definitions = definitions;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getCodomain() {
        return codomain;
    }

    public void setCodomain(String codomain) {
        this.codomain = codomain;
    }

}