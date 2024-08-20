/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Buddhika
 * http://www.nlm.nih.gov/research/umls/META3_current_semantic_types.html
 */
public enum SymanticHyrachi {
//http://www.nlm.nih.gov/research/umls/META3_current_semantic_types.html

    Symantic(null),
    Symantic_Type_Entity(Symantic),
    Financial_Entity(Symantic),
    Physical_Object(Symantic_Type_Entity),
    Organism(Physical_Object),
    Plant(Organism),
    Fungus(Organism),

    Virus(Organism),
    Bacterium(Organism),
    Archaeon(Organism),
    Eukaryote(Organism),
    Animal(Eukaryote),
    Vertebrate(Animal),
    Amphibian(Vertebrate),
    Bird(Vertebrate),
    Fish(Vertebrate),
    Reptile(Vertebrate),
    Mammal(Vertebrate),
    Human(Mammal),
    Anatomical_Structure(Physical_Object),
    Embryonic_Structure(Anatomical_Structure),
    Anatomical_Abnormality(Anatomical_Structure),
    Congenital_Abnormality(Anatomical_Abnormality),
    Acquired_Abnormality(Anatomical_Abnormality),
    Fully_Formed_Anatomical_Structure(Anatomical_Structure),
    Body_Part_Organ_or_Organ_Component(Fully_Formed_Anatomical_Structure),
    Tissue(Fully_Formed_Anatomical_Structure),
    Cell(Fully_Formed_Anatomical_Structure),
    Cell_Component(Fully_Formed_Anatomical_Structure),
    Gene_or_Genome(Fully_Formed_Anatomical_Structure),
    Manufactured_Object(Physical_Object),
    Medical_Device(Manufactured_Object),
    Drug_Delivery_Device(Medical_Device),
    Research_Device(Manufactured_Object),
    Clinical_Drug(Manufactured_Object),
    Substance(Physical_Object),
    Chemical(Physical_Object),
    Chemical_Viewed_Functionally(Chemical),
    Pharmacologic_Substance(Chemical_Viewed_Functionally),
    Antibiotic(Pharmacologic_Substance),
    Biomedical_or_Dental_Material(Chemical_Viewed_Functionally),
    Biologically_Active_Substance(Chemical_Viewed_Functionally),
    Neuroreactive_Substance_or_Biogenic_Amine(Biologically_Active_Substance),
    Hormone(Biologically_Active_Substance),
    Enzyme(Biologically_Active_Substance),
    Vitamin(Biologically_Active_Substance),
    Immunologic_Factor(Biologically_Active_Substance),
    Receptor(Biologically_Active_Substance),
    Indicator_Reagent_or_Diagnostic_Acid(Chemical_Viewed_Functionally),
    Hazardous_or_Poisonous_Substance(Chemical_Viewed_Functionally),
    Chemical_Viewed_Structurally(Chemical),
    Organic_Chemical(Chemical_Viewed_Structurally),
    Nucleic_Acid_Nucleoside_or_Nucleotide(Organic_Chemical),
    Organophosphorus_Compound(Organic_Chemical),
    Amino_Acid_Peptide_or_Protein(Organic_Chemical),
    Carbohydrate(Organic_Chemical),
    Lipid(Organic_Chemical),
    Steroid(Lipid),
    Eicosanoid(Lipid),
    Inorganic_Chemical(Chemical_Viewed_Structurally),
    Element_Ion_or_Isotope(Chemical_Viewed_Structurally),
    BodySubstance(Physical_Object),
    Food(Physical_Object),
    ConceptualEntity(Symantic_Type_Entity),
    Idea_or_Concept(ConceptualEntity),
    Temporal_Concept(Idea_or_Concept),
    Qualitative_Concept(Idea_or_Concept),
    Quantitative_Concept(Idea_or_Concept),
    Functional_Concept(Idea_or_Concept),
    Body_System(Functional_Concept),
    Spatial_Concept(Idea_or_Concept),
    Body_Space_or_Junction(Spatial_Concept),
    Body_Location_or_Region(Spatial_Concept),
    Molecular_Sequence(Spatial_Concept),
    Nucleotide_Sequence(Molecular_Sequence),
    Amino_Acid_Sequence(Molecular_Sequence),
    Carbohydrate_Sequence(Molecular_Sequence),
    Geographic_Area(Spatial_Concept),
    Finding(ConceptualEntity),
    Laboratory_or_Test_Result(Finding),
    Sign_or_Symptom(Finding),
    Organism_Attribute(ConceptualEntity),
    Clinical_Attribute(Organism_Attribute),
    Intellectual_Product(ConceptualEntity),
    Classification(Intellectual_Product),
    Regulation_or_Law(Intellectual_Product),
    Language(ConceptualEntity),
    Occupation_or_Discipline(ConceptualEntity),
    Biomedical_Occupation_or_Discipline(Occupation_or_Discipline),
    Organization(ConceptualEntity),
    Health_Care_Related_Organization(Organization),
    Professional_Society(Organization),
    Self_help_or_Relief_Organization(Organization),
    Group_Attribute(ConceptualEntity),
    Group(ConceptualEntity),
    Professional_or_Occupational_Group(Group),
    Population_Group(Group),
    Family_Group(Group),
    Age_Group(Group),
    Patient_or_Disabled_Group(Group),
    Symantic_Type_Event(Symantic),
    Activity(Symantic_Type_Event),
    Behavior(Symantic_Type_Event),
    Social_Behavior(Behavior),
    Individual_Behavior(Behavior),
    Daily_or_Recreational_Activity(Symantic_Type_Event),
    Occupational_Activity(Symantic_Type_Event),
    Health_Care_Activity(Occupational_Activity),
    Laboratory_Procedure(Health_Care_Activity),
    Diagnostic_Procedure(Health_Care_Activity),
    Therapeutic_or_Preventive_Procedure(Health_Care_Activity),
    Research_Activity(Symantic_Type_Event),
    Molecular_Biology_Research_Technique(Research_Activity),
    Governmental_or_Regulatory_Activity(Symantic_Type_Event),
    Educational_Activity(Symantic_Type_Event),
    Machine_Activity(Symantic_Type_Event),
    Phenomenon_or_Process(Symantic_Type_Event),
    Human_caused_Phenomenon_or_Process(Phenomenon_or_Process),
    Environmental_Effect_of_Humans(Phenomenon_or_Process),
    Natural_Phenomenon_or_Process(Phenomenon_or_Process),
    Biologic_Function(Natural_Phenomenon_or_Process),
    Physiologic_Function(Biologic_Function),
    Organism_Function(Physiologic_Function),
    Mental_Process(Organism_Function),
    Organ_or_Tissue_Function(Physiologic_Function),
    Cell_Function(Physiologic_Function),
    Molecular_Function(Physiologic_Function),
    Genetic_Function(Molecular_Function),
    Pathologic_Function(Biologic_Function),
    Disease_or_Syndrome(Pathologic_Function),
    Mental_or_Behavioral_Dysfunction(Disease_or_Syndrome),
    Neoplastic_Process(Disease_or_Syndrome),
    Cell_or_Molecular_Dysfunction(Pathologic_Function),
    Experimental_Model_of_Disease(Pathologic_Function),
    Injury_or_Poisoning(Phenomenon_or_Process),
    Fee_List_Type(Financial_Entity),
    ;

    private final SymanticHyrachi parent;

    SymanticHyrachi(SymanticHyrachi parent) {
        this.parent = parent;
        if (this.parent != null) {
            this.parent.addChild(this);
        }
    }

    private final List<SymanticHyrachi> children = new ArrayList<>();

    public SymanticHyrachi[] children() {
        return children.toArray(new SymanticHyrachi[children.size()]);
    }

    public SymanticHyrachi[] allChildren() {
        List<SymanticHyrachi> list = new ArrayList<>();
        addChildren(this, list);
        return list.toArray(new SymanticHyrachi[list.size()]);
    }

    private static void addChildren(SymanticHyrachi root, List<SymanticHyrachi> list) {
        list.addAll(root.children);
        for (SymanticHyrachi child : root.children) {
            addChildren(child, list);
        }
    }

    private void addChild(SymanticHyrachi child) {
        this.children.add(child);
    }

    public boolean is(SymanticHyrachi other) {
        if (other == null) {
            return false;
        }

        for (SymanticHyrachi t = this; t != null; t = t.parent) {
            if (other == t) {
                return true;
            }
        }
        return false;
    }
}
