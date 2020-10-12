import java.util.Scanner;

import javax.lang.model.util.ElementScanner6;

import java.util.Map;

//import jdk.nashorn.internal.runtime.regexp.joni.Regex;

import java.util.ArrayList;
import java.util.HashMap;
import java.awt.List;
import java.lang.Exception;

public class RegEx {
  //MACROS
  static final int CONCAT = 0xC04CA7;
  static final int ETOILE = 0xE7011E;
  static final int ALTERN = 0xA17E54;
  static final int PROTECTION = 0xBADDAD;

  static final int PARENTHESEOUVRANT = 0x16641664;
  static final int PARENTHESEFERMANT = 0x51515151;
  static final int DOT = 0xD07;
  
  //REGEX
  private static String regEx;
  
  //CONSTRUCTOR
  public RegEx(){}

  //MAIN
  public static void main(String arg[]) {
    System.out.println("Welcome to Bogota, Mr. Thomas Anderson.");
    if (arg.length!=0) {
      regEx = arg[0];
    } else {
      Scanner scanner = new Scanner(System.in);
      System.out.print("  >> Please enter a regEx: ");
      regEx = scanner.next();
    }
    System.out.println("  >> Parsing regEx \""+regEx+"\".");
    System.out.println("  >> ...");
    
    if (regEx.length()<1) {
      System.err.println("  >> ERROR: empty regEx.");
    } else {
      System.out.print("  >> ASCII codes: ["+(int)regEx.charAt(0));
      for (int i=1;i<regEx.length();i++) System.out.print(","+(int)regEx.charAt(i));
      System.out.println("].");
      try {
        RegExTree ret = parse();
        Automata a = new Automata(ret);
        System.out.println(a.toString());
        System.out.println(a.toStringTab());
        System.out.println("  >> Tree result: "+ret.toString()+".");
      } catch (Exception e) {
    	  e.printStackTrace();
    	  
        System.err.println("  >> ERROR: syntax error for regEx \""+regEx+"\"."+e+e.getCause());
      }
    }
    

    System.out.println("  >> ...");
    System.out.println("  >> Parsing completed.");
    System.out.println("Goodbye Mr. Anderson.");
  }

  //FROM REGEX TO SYNTAX TREE
  private static RegExTree parse() throws Exception {
    //BEGIN DEBUG: set conditionnal to true for debug example
    if (false) throw new Exception();
    RegExTree example = exampleAhoUllman();
    if (false) return example;
    //END DEBUG

    ArrayList<RegExTree> result = new ArrayList<RegExTree>();
    for (int i=0;i<regEx.length();i++) result.add(new RegExTree(charToRoot(regEx.charAt(i)),new ArrayList<RegExTree>()));
    
    return parse(result);
  }
  private static int charToRoot(char c) {
    if (c=='.') return DOT;
    if (c=='*') return ETOILE;
    if (c=='|') return ALTERN;
    if (c=='(') return PARENTHESEOUVRANT;
    if (c==')') return PARENTHESEFERMANT;
    return (int)c;
  }
  private static RegExTree parse(ArrayList<RegExTree> result) throws Exception {
    while (containParenthese(result)) result=processParenthese(result);
    while (containEtoile(result)) result=processEtoile(result);
    while (containConcat(result)) result=processConcat(result);
    while (containAltern(result)) result=processAltern(result);

    if (result.size()>1) throw new Exception();

    return removeProtection(result.get(0));
  }
  private static boolean containParenthese(ArrayList<RegExTree> trees) {
    for (RegExTree t: trees) if (t.root==PARENTHESEFERMANT || t.root==PARENTHESEOUVRANT) return true;
    return false;
  }
  private static ArrayList<RegExTree> processParenthese(ArrayList<RegExTree> trees) throws Exception {
    ArrayList<RegExTree> result = new ArrayList<RegExTree>();
    boolean found = false;
    for (RegExTree t: trees) {
      if (!found && t.root==PARENTHESEFERMANT) {
        boolean done = false;
        ArrayList<RegExTree> content = new ArrayList<RegExTree>();
        while (!done && !result.isEmpty())
          if (result.get(result.size()-1).root==PARENTHESEOUVRANT) { done = true; result.remove(result.size()-1); }
          else content.add(0,result.remove(result.size()-1));
        if (!done) throw new Exception();
        found = true;
        ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
        subTrees.add(parse(content));
        result.add(new RegExTree(PROTECTION, subTrees));
      } else {
        result.add(t);
      }
    }
    if (!found) throw new Exception();
    return result;
  }
  private static boolean containEtoile(ArrayList<RegExTree> trees) {
    for (RegExTree t: trees) if (t.root==ETOILE && t.subTrees.isEmpty()) return true;
    return false;
  }
  private static ArrayList<RegExTree> processEtoile(ArrayList<RegExTree> trees) throws Exception {
    ArrayList<RegExTree> result = new ArrayList<RegExTree>();
    boolean found = false;
    for (RegExTree t: trees) {
      if (!found && t.root==ETOILE && t.subTrees.isEmpty()) {
        if (result.isEmpty()) throw new Exception();
        found = true;
        RegExTree last = result.remove(result.size()-1);
        ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
        subTrees.add(last);
        result.add(new RegExTree(ETOILE, subTrees));
      } else {
        result.add(t);
      }
    }
    return result;
  }
  private static boolean containConcat(ArrayList<RegExTree> trees) {
    boolean firstFound = false;
    for (RegExTree t: trees) {
      if (!firstFound && t.root!=ALTERN) { firstFound = true; continue; }
      if (firstFound) if (t.root!=ALTERN) return true; else firstFound = false;
    }
    return false;
  }
  private static ArrayList<RegExTree> processConcat(ArrayList<RegExTree> trees) throws Exception {
    ArrayList<RegExTree> result = new ArrayList<RegExTree>();
    boolean found = false;
    boolean firstFound = false;
    for (RegExTree t: trees) {
      if (!found && !firstFound && t.root!=ALTERN) {
        firstFound = true;
        result.add(t);
        continue;
      }
      if (!found && firstFound && t.root==ALTERN) {
        firstFound = false;
        result.add(t);
        continue;
      }
      if (!found && firstFound && t.root!=ALTERN) {
        found = true;
        RegExTree last = result.remove(result.size()-1);
        ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
        subTrees.add(last);
        subTrees.add(t);
        result.add(new RegExTree(CONCAT, subTrees));
      } else {
        result.add(t);
      }
    }
    return result;
  }
  private static boolean containAltern(ArrayList<RegExTree> trees) {
    for (RegExTree t: trees) if (t.root==ALTERN && t.subTrees.isEmpty()) return true;
    return false;
  }
  private static ArrayList<RegExTree> processAltern(ArrayList<RegExTree> trees) throws Exception {
    ArrayList<RegExTree> result = new ArrayList<RegExTree>();
    boolean found = false;
    RegExTree gauche = null;
    boolean done = false;
    for (RegExTree t: trees) {
      if (!found && t.root==ALTERN && t.subTrees.isEmpty()) {
        if (result.isEmpty()) throw new Exception();
        found = true;
        gauche = result.remove(result.size()-1);
        continue;
      }
      if (found && !done) {
        if (gauche==null) throw new Exception();
        done=true;
        ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
        subTrees.add(gauche);
        subTrees.add(t);
        result.add(new RegExTree(ALTERN, subTrees));
      } else {
        result.add(t);
      }
    }
    return result;
  }
  private static RegExTree removeProtection(RegExTree tree) throws Exception {
    if (tree.root==PROTECTION && tree.subTrees.size()!=1) throw new Exception();
    if (tree.subTrees.isEmpty()) return tree;
    if (tree.root==PROTECTION) return removeProtection(tree.subTrees.get(0));

    ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
    for (RegExTree t: tree.subTrees) subTrees.add(removeProtection(t));
    return new RegExTree(tree.root, subTrees);
  }
  
  //EXAMPLE
  // --> RegEx from Aho-Ullman book Chap.10 Example 10.25
  private static RegExTree exampleAhoUllman() {
    RegExTree a = new RegExTree((int)'a', new ArrayList<RegExTree>());
    RegExTree b = new RegExTree((int)'b', new ArrayList<RegExTree>());
    RegExTree c = new RegExTree((int)'c', new ArrayList<RegExTree>());
    ArrayList<RegExTree> subTrees = new ArrayList<RegExTree>();
    subTrees.add(c);
    RegExTree cEtoile = new RegExTree(ETOILE, subTrees);
    subTrees = new ArrayList<RegExTree>();
    subTrees.add(b);
    subTrees.add(cEtoile);
    RegExTree dotBCEtoile = new RegExTree(CONCAT, subTrees);
    subTrees = new ArrayList<RegExTree>();
    subTrees.add(a);
    subTrees.add(dotBCEtoile);
    return new RegExTree(ALTERN, subTrees);
  }
}

//UTILITARY CLASS
class RegExTree {
  protected int root;
  protected ArrayList<RegExTree> subTrees;
  public RegExTree(int root, ArrayList<RegExTree> subTrees) {
    this.root = root;
    this.subTrees = subTrees;
  }
  //FROM TREE TO PARENTHESIS
  public String toString() {
    if (subTrees.isEmpty()) return rootToString();
    String result = rootToString()+"("+subTrees.get(0).toString();
    System.out.println("size == "+subTrees.size());
    for (int i=1;i<subTrees.size();i++) result+=","+subTrees.get(i).toString();
    for(int i=1; i<subTrees.size();i++) result+="";
    return result+")";
  }
  private String rootToString() {
    if (root==RegEx.CONCAT) return ".";
    if (root==RegEx.ETOILE) return "*";
    if (root==RegEx.ALTERN) return "|";
    if (root==RegEx.DOT) return ".";
    return Character.toString((char)root);
  }

  
  public ArrayList<RegExTree> getSubTrees(){
      return this.subTrees;
  }

  public int getRoot()
  {
      return this.root;
  }
}


//Classe representant les noeuds d'un automate non deterministe
class AutomataNodeND {
	
	public int id;
	public boolean acceptance; //determine si le noeud est celui d'acceptation de l'automate
	public Map<Integer,ArrayList<AutomataNodeND>> transitions; //dictionnaire des transtions liée aux etats d'arrivees
	
	public AutomataNodeND(int id) {
		this.id = id;
		this.acceptance = false;
		this.transitions = new HashMap<Integer,ArrayList<AutomataNodeND>>();
	}
	
	public void setAcceptance() {
		this.acceptance = true;
	}
	
	public boolean isAcceptance() {
		return this.acceptance;
	}
	
	public int getId() {
		return this.id;
	}
	
	public void addTransition(int trans, AutomataNodeND arr) {
		if(transitions.containsKey(trans)) {
			transitions.get(trans).add(arr);
		}
		else {
			ArrayList<AutomataNodeND> ann = new ArrayList<AutomataNodeND>();
			ann.add(arr);
			transitions.put(trans,ann);
		}
	}
	
	public ArrayList<AutomataNodeND> getTransition(int trans){
		return this.transitions.get(trans);
	}
	
	public Map<Integer,ArrayList<AutomataNodeND>> getTransitions(){
		return this.transitions;
	}
}

//Classe representant le noeud d'un automate deterministe
class AutomataNodeD{
	public ArrayList<AutomataNodeND> access; //repertorie l ensemble des etats accessible mais deja parmi les ancetre
	public ArrayList<Integer> ancetres; //repertorie l ensemble des entiers deja rencontrés
	public ArrayList<Integer> courant; //repertorie les etats courant
	public Map<Integer,AutomataNodeD> liens;
	public boolean acceptance; //determine si le noeud est celui d'acceptation de l'automate
	public boolean recursif; //determine si le noeud est recursif
	
	public AutomataNodeD(ArrayList<Integer> ancetres) {
		this.ancetres = ancetres;
		this.acceptance = false;
		this.recursif = false;
		this.courant = new ArrayList<Integer>();
		this.access = new ArrayList<AutomataNodeND>();
		this.liens = new HashMap<Integer,AutomataNodeD>();
	}
	
	public void addAccess(AutomataNodeND i) {
		this.access.add(i);
	}
	
	public boolean isAccessible(AutomataNodeND i) {
		return this.access.contains(i);
	}
	
	public ArrayList<Integer> getAncetres(){
		return this.ancetres;
	}
	

	public void setAcceptance() {
		this.acceptance = true;
	}
	
	public boolean isAcceptance() {
		return this.acceptance;
	}
	
	public void setRecursif() {
		this.recursif = true;
	}
	
	public boolean isRecursif() {
		return this.recursif;
	}
	
	public void addInt(int i) {
		courant.add(i);
	}
	
	public void setLinks(ArrayList<Integer> links) {
		this.courant = links;
	}
	
	public void addLink(int trans, AutomataNodeD link) {
		this.liens.put(trans, link);
	}
	public void delLink(int trans) {
		this.liens.remove(trans);
	}
	
	public ArrayList<Integer> getCourant(){
		return this.courant;
	}
	public Map<Integer,AutomataNodeD> getLinks(){
		return this.liens;
	}
	public AutomataNodeD getLink(int trans){
		return this.liens.get(trans);
	}
	public boolean isAncestre(int i) {
		return this.ancetres.contains(i);
	}
}


class Automata 
{
	private static final int ID_EPSILON_TRANSITION = -1;
	
	private AutomataNodeND start_node;
    private AutomataNodeND final_node;
    private int id_node;
    private AutomataNodeD racine_det; // premier noeud du tableau deterministe
    private ArrayList<Integer> transitions_c; //liste des transitions de l'automate

    public Automata(RegExTree mytree){
    	id_node = 0;
        start_node = new AutomataNodeND(id_node);
        id_node++;
        final_node = new AutomataNodeND(id_node);
        id_node++;
        final_node.setAcceptance();
        
        this.transitions_c = new ArrayList<Integer>();
        racine_det = new AutomataNodeD(new ArrayList<Integer>());
        toAutomata(mytree,start_node,final_node);
        detTabStart();
        
    }
    
    public String ArraytoString(ArrayList<AutomataNodeND> an) {
    	String chaine = "";
    	for(AutomataNodeND a : an) {
    		chaine += " _ " +a.getId(); 
    	}
    	return chaine;
    }
    
    public String toStringRec(AutomataNodeND aut, ArrayList<Integer> deja) {
    	String chaine = "";
    	if(!deja.contains(aut.getId())) {
    		chaine += aut.getId();
    		deja.add(aut.getId());
    		for(int a : aut.getTransitions().keySet()) {
    			chaine += "  n"+ (char)a+" -> "+ ArraytoString(aut.getTransition(a));
    		}
    		chaine += "    \n";
    		for(int a : aut.getTransitions().keySet()) {
    			for(AutomataNodeND au : aut.getTransition(a)) {
        			chaine += toStringRec(au, deja);
        		}
    		}
    	}
    	return chaine;
    }
    
    public String toStringRecTab(AutomataNodeD n, String chemin) {
    	String chaine = "";
    	
    	if((!n.isRecursif()) && (!n.isAcceptance())){
    		chaine += chemin + " : ";
    	} else if(n.isRecursif() && n.isAcceptance()) {
    		chaine += chemin + "-RF" + " : ";
    	} else if(n.isRecursif()) {
    		chaine += chemin + "-R" + " : ";
    	} else {
    		chaine += chemin + "-F" + " : ";
    	}
    	for(int l : n.getCourant()) {
    		chaine += " _ "+l;
    	}
    	chaine += "\n";
    	
    	for(int noeudi : n.getLinks().keySet()) {
    		chaine += toStringRecTab(n.getLink(noeudi),chemin+"-"+(char)noeudi );
    	}
    	
    	return chaine;
    }
    
    public String toString() {
    	String chaine = "Chaque ligne correspond a la liste des liens d'un noeud.\n n-1 correspond a la transition epsilon.\n";
    	ArrayList<Integer> deja = new ArrayList<Integer>();//cette arraylist servira a identifier les noeuds deja rencontree

    	return chaine+toStringRec(start_node, deja);
    }
    
    public String toStringTab() {
    	String chaine = "Chaque ligne correspond a un noeud suivie par les etats de ce noeud.\n\n";
    	ArrayList<Integer> deja = new ArrayList<Integer>();//cette arraylist servira a identifier les noeuds deja rencontree
    	return chaine+toStringRecTab(this.racine_det,"0");
    }


    private void toAutomata(RegExTree tree, AutomataNodeND start_node, AutomataNodeND final_node)
    {	
    	//Cas où il s'agit d'une operation
    	if (tree.getRoot()==RegEx.CONCAT || tree.getRoot()==RegEx.DOT) {
    		AutomataNodeND node1 = new AutomataNodeND(id_node);
    		id_node++;
    		AutomataNodeND node2 = new AutomataNodeND(id_node);
    		id_node++;
    		node1.addTransition(ID_EPSILON_TRANSITION, node2);
    		toAutomata(tree.getSubTrees().get(0),start_node,node1);
    		toAutomata(tree.getSubTrees().get(1),node2,final_node);
    	}
        if (tree.getRoot()==RegEx.ETOILE) {
        	AutomataNodeND node1 = new AutomataNodeND(id_node);
    		id_node++;
    		AutomataNodeND node2 = new AutomataNodeND(id_node);
    		id_node++;
    		node2.addTransition(ID_EPSILON_TRANSITION, node1);
    		start_node.addTransition(ID_EPSILON_TRANSITION, node1);
    		node2.addTransition(ID_EPSILON_TRANSITION, final_node);
    		start_node.addTransition(ID_EPSILON_TRANSITION, final_node);
    		toAutomata(tree.getSubTrees().get(0),node1,node2);
        }
        if (tree.getRoot()==RegEx.ALTERN) {
        	AutomataNodeND node1 = new AutomataNodeND(id_node);
    		id_node++;
    		AutomataNodeND node2 = new AutomataNodeND(id_node);
    		id_node++;
    		AutomataNodeND node3 = new AutomataNodeND(id_node);
    		id_node++;
    		AutomataNodeND node4 = new AutomataNodeND(id_node);
    		id_node++;
    		
    		start_node.addTransition(ID_EPSILON_TRANSITION, node1);
    		start_node.addTransition(ID_EPSILON_TRANSITION, node3);
    		
    		node2.addTransition(ID_EPSILON_TRANSITION, final_node);
    		node4.addTransition(ID_EPSILON_TRANSITION, final_node);
    		toAutomata(tree.getSubTrees().get(0),node1,node2);
    		toAutomata(tree.getSubTrees().get(1),node3,node4);
        }
        
    	//Cas où il s'agit d'une feuille
    	if(tree.getSubTrees().isEmpty()) {
    		start_node.addTransition(tree.getRoot(),final_node);
    		this.transitions_c.add(tree.getRoot());
    	}
    	
    }
    
    public void detTabStart() {
        ArrayList<AutomataNodeND> ancetres = new ArrayList<AutomataNodeND>();
        ArrayList<Integer> anc = new ArrayList<Integer>();
        ancetres.add(start_node);
    	detTab(ancetres, this.racine_det, ID_EPSILON_TRANSITION);
    	
    	for(AutomataNodeND c : ancetres) {
			anc.add(c.getId());
		}
    	
    	for(int trans : this.transitions_c) {
    		AutomataNodeD noeud = new AutomataNodeD(anc);
    		this.racine_det.addLink(trans, noeud);
    		detTab((ArrayList<AutomataNodeND>) ancetres.clone(), noeud, trans);
    		if(noeud.getCourant().isEmpty()) {
    			this.racine_det.delLink(trans);
    		}
    	}
    }
    
    //determination e l'automate deterministe
    public void detTab(ArrayList<AutomataNodeND> ancetres, AutomataNodeD noeud, int current_link) {
    	ArrayList<AutomataNodeND> cur = new ArrayList<AutomataNodeND>();
    	int i;
    	
    	//On ajoute les noeuds accessible depuis le lien ajouté par les ancetres direct
    	for(AutomataNodeND a : ancetres) {
    		for(int link : a.getTransitions().keySet()) {
    			for(AutomataNodeND l : a.getTransition(link)) {
	    			if((!ancetres.contains(l)) && (link == current_link || link == ID_EPSILON_TRANSITION) && (!cur.contains(l))) {
	    				 cur.add(l);
	    			} else if(link == current_link || link == ID_EPSILON_TRANSITION){
	    				noeud.addAccess(l);
	    			}
    			}
    		}
    	}
    	
    	
    	//On ajoute les noeuds accessible depuis les noeuds courants
    	for(i=0;i<cur.size();i++) { 
    		for(int link : cur.get(i).getTransitions().keySet()) {
    			for(AutomataNodeND l : cur.get(i).getTransition(link)) {
	    			if((!ancetres.contains(l)) && (link == ID_EPSILON_TRANSITION) && (!cur.contains(l)) ) {
	    				cur.add(l);
	    			} 
    			}
    		}
    	}
    	
    	for(int y = 0;y<noeud.access.size();y++) {
    		ArrayList<AutomataNodeND> al = noeud.access.get(y).getTransition(ID_EPSILON_TRANSITION);
    		
    		if(al != null) {
    			if(noeud.access.get(y).getTransition(current_link)!=null)
    				al.addAll(noeud.access.get(y).getTransition(current_link));
	    		for(AutomataNodeND a1 : al) {
			    	noeud.addAccess(a1);
			    }
	    		
	    		
    		}
    		else {
    			if(noeud.access.get(y).getTransition(current_link)!=null)
    				al = noeud.access.get(y).getTransition(current_link);
    			else
    				break;
	    		for(AutomataNodeND a1 : al) {
			    	noeud.addAccess(a1);
			    }
    		}
    		
    	}
    	
    	
    	//On considere toute les connections possibles si cur non vide
    	if(cur.isEmpty()) {
    		return;
    	}
    	else{
    		ArrayList<Integer> anc = new ArrayList<Integer>();
    		//On met a jour ancetre
    		int o = 0;
    		for(AutomataNodeND c : cur) {
    			ancetres.add(c);
    			if(noeud.getAncetres().contains(c.getId()))
    				o++;
    			anc.add(c.getId());
    		}
    		noeud.setLinks((ArrayList<Integer>) anc.clone());
    		for(AutomataNodeND c : ancetres) {
    			anc.add(c.getId());
    			
    			if(c.isAcceptance()) {
    				noeud.setAcceptance();
    			}
    		}
    		if(o==cur.size())
    			return;
    		for(int ti : this.transitions_c) {
    			AutomataNodeD noeud1 = new AutomataNodeD(anc);
	    		noeud.addLink(ti, noeud1);
	    		detTab((ArrayList<AutomataNodeND>) cur.clone(), noeud1, ti);
	    		int k =0;
	    		
	    		for(AutomataNodeND a : cur) {
	    			if(noeud1.isAccessible(a)) k++;
	    		}
	        	
	    		if(k==cur.size()) {
	    			noeud.setRecursif();
	    		}
	    		
	    		if(noeud1.getCourant().isEmpty()) {
	    			noeud.delLink(ti);
	    		}
    		}
    	}
    }
}
