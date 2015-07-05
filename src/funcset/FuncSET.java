package funcset;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FuncSET {

    public static void main(String[] args) {
        SETParser setParser = new SETParser();
        //MUISTA MIINUSMERKIT
        //MUISTA UNIONIT SUN MUUT
        /*setParser.eval("f:Set->Set");
        setParser.eval("f(x)=union[{1},{f}]");
        setParser.eval("?- f[{20}]");*/
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String text = "";
        "quit".intern();
        do {
            try {
                System.out.print(">: ");
                text = br.readLine().intern();
                boolean t=setParser.eval(text);
                System.out.println(t);
            } catch (IOException ex) {
                Logger.getLogger(FuncSET.class.getName()).log(Level.SEVERE, null, ex);
            }
        } while (text != "quit");
    }

}
