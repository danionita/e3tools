/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package e3fraud.tools;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 *
 * @author Dan
 */
public class SettingsObjects {

    public static class AdvancedGenerationSettings {

        private boolean generateHidden, generateNonOccurring, generateCollusion;
        private int colludingActors, numberOfHiddenTransfersPerExchange;

        public AdvancedGenerationSettings(boolean generateHidden, boolean generateNonOccurring, boolean generateCollusion, int collusions, int numberOfHiddenTransfersPerExchange) {
            this.generateHidden = generateHidden;
            this.generateNonOccurring = generateNonOccurring;
            this.generateCollusion = generateCollusion;
            this.colludingActors = collusions;
            this.numberOfHiddenTransfersPerExchange = numberOfHiddenTransfersPerExchange;
        }

        /**
         * Creates a advancedGenerationSettings object with default values
         */
        public AdvancedGenerationSettings() {
            generateHidden = true;
            generateNonOccurring = true;
            generateCollusion = true;
            colludingActors = 2;
            numberOfHiddenTransfersPerExchange = 2;
        }

        public boolean isGenerateHidden() {
            return generateHidden;
        }

        public void setGenerateHidden(boolean generateHidden) {
            this.generateHidden = generateHidden;
        }

        public boolean isGenerateNonOccurring() {
            return generateNonOccurring;
        }

        public void setGenerateNonOccurring(boolean generateNonOccurring) {
            this.generateNonOccurring = generateNonOccurring;
        }

        public boolean isGenerateCollusion() {
            return generateCollusion;
        }

        public void setGenerateCollusion(boolean generateCollusion) {
            this.generateCollusion = generateCollusion;
        }

        public int getColludingActors() {
            return colludingActors;
        }

        public void setColludingActors(int colludingActors) {
            this.colludingActors = colludingActors;
        }

        public int getNumberOfHiddenTransfersPerExchange() {
            return numberOfHiddenTransfersPerExchange;
        }

        public void setNumberOfHiddenTransfersPerExchange(int numberOfHiddenTransfersPerExchange) {
            this.numberOfHiddenTransfersPerExchange = numberOfHiddenTransfersPerExchange;
        }

    }

    public static class GenerationSettings {
        //TODO: use this object in the rest of the code

        private String selectedActorString, selectedNeedString;
        private Resource selectedActor, selectedNeed;
        private int startValue, endValue;

        /**
         *
         * @param selectedActorString the main actor's name
         * @param selectedActor the main actor's RDF resource
         * @param selectedNeed the selected need's RDF resource
         * @param selectedNeedString the selected need's name
         * @param startValue the min occurrence rate of need
         * @param endValue the max occurrence rate of need
         * 
         */
        public GenerationSettings(String selectedActorString, String selectedNeedString, Resource selectedActor, Resource selectedNeed, int startValue, int endValue) {
            this.selectedActorString = selectedActorString;
            this.selectedNeedString = selectedNeedString;
            this.selectedActor = selectedActor;
            this.selectedNeed = selectedNeed;
            this.startValue = startValue;
            this.endValue = endValue;
        }

        /**
         * Creates a advancedGenerationSettings object with empty values
         */
        public GenerationSettings() {
        }

        public String getSelectedActorString() {
            return selectedActorString;
        }

        public void setSelectedActorString(String selectedActorString) {
            this.selectedActorString = selectedActorString;
        }

        public String getSelectedNeedString() {
            return selectedNeedString;
        }

        public void setSelectedNeedString(String selectedNeedString) {
            this.selectedNeedString = selectedNeedString;
        }

        public Resource getSelectedActor() {
            return selectedActor;
        }

        public void setSelectedActor(Resource selectedActor) {
            this.selectedActor = selectedActor;
        }

        public Resource getSelectedNeed() {
            return selectedNeed;
        }

        public void setSelectedNeed(Resource selectedNeed) {
            this.selectedNeed = selectedNeed;
        }

        public int getStartValue() {
            return startValue;
        }

        public void setStartValue(int startValue) {
            this.startValue = startValue;
        }

        public int getEndValue() {
            return endValue;
        }

        public void setEndValue(int endValue) {
            this.endValue = endValue;
        }
    }

    public static class SortingAndGroupingSettings {

        //TODO: use this object in the rest of the code 
        private int sortCriteria, groupingCriteria;

        /**
         * @param sortCriteria 0 - do not sort, 1 - sort by loss first, 2- sort
         * by gain first
         * @param groupingCriteria 0 - do not group, 1 - group based on
         * generated collusion groups
         */
        public SortingAndGroupingSettings(int sortCriteria, int groupingCriteria) {
            this.sortCriteria = sortCriteria;
            this.groupingCriteria = groupingCriteria;
        }

        /**
         * Creates a advancedGenerationSettings object with default values
         */
        public SortingAndGroupingSettings() {
            this.sortCriteria = 1;
            this.groupingCriteria = 0;
        }
        

        public int getSortCriteria() {
            return sortCriteria;
        }

        public void setSortCriteria(int sortCriteria) {
            this.sortCriteria = sortCriteria;
        }

        public int getGroupingCriteria() {
            return groupingCriteria;
        }

        public void setGroupingCriteria(int groupingCriteria) {
            this.groupingCriteria = groupingCriteria;
        }
    }

    public static class FilteringSettings {
        //TODO: use this object in the rest of the code

        private Double lossMin, lossMax, gainMin, gainMax;

        /**
         *
         * @param lossMin Filter by loss - minimum value
         * @param lossMax Filter by loss - maximum value
         * @param gainMin Filter by gain - minimum value
         * @param gainMax Filter by gain - maximum value
         */
        public FilteringSettings(Double lossMin, Double lossMax, Double gainMin, Double gainMax) {
            this.lossMin = lossMin;
            this.lossMax = lossMax;
            this.gainMin = gainMin;
            this.gainMax = gainMax;
        }

        /**
         * Creates a advancedGenerationSettings object with default values
         */
        public FilteringSettings() {
            this.lossMin = 0.0;
            this.gainMin = 0.0;
        }

        public void clearFilters(){
        this.lossMin = -Double.MAX_VALUE;
        this.lossMax = Double.MAX_VALUE;
        this.gainMin = -Double.MAX_VALUE;
        this.gainMax = Double.MAX_VALUE;
        }

                
        public Double getLossMin() {
            return lossMin;
        }

        public void setLossMin(Double lossMin) {
            this.lossMin = lossMin;
        }

        public Double getLossMax() {
            return lossMax;
        }

        public void setLossMax(Double lossMax) {
            this.lossMax = lossMax;
        }

        public Double getGainMin() {
            return gainMin;
        }

        public void setGainMin(Double gainMin) {
            this.gainMin = gainMin;
        }

        public Double getGainMax() {
            return gainMax;
        }

        public void setGainMax(Double gainMax) {
            this.gainMax = gainMax;
        }
    }
}
