package ld.sa_backend.wrapper;

/*
 * Classe simple pour représenter une évaluation de sentiment renvoyée par le modèle NLP.
 * Elle contient un label (ex: "1 star", "2 stars", etc.) et un score de confiance associé.
*/

public class Rating {

    private String label;
    private double score;

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    @Override
    public String toString() {
        return "Rating{" + "label='" + label + '\'' + ", score=" + score + '}';
    }
    
}
