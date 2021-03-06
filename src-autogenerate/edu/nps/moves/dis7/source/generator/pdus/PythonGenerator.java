/**
 * Copyright (c) 2008-2021, MOVES Institute, Naval Postgraduate School (NPS). All rights reserved.
 * This work is provided under a BSD open-source license, see project license.html and license.txt
 */
package edu.nps.moves.dis7.source.generator.pdus;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Map;
/* not thouroughly examined, global change: VARIABLE_LIST to OBJECT_LIST and FIXED_LIST to PRIMITIVE_LIST */
/**
 * Constructor, not fully implemented.
 * @author DMcG
 */
public class PythonGenerator extends Generator
{
    /** Standard python indent is four spaces */
    public String INDENT="    ";
    /** Properties of interest */
    public Properties   marshalTypes = new Properties();
    /** Properties of interest */
    public Properties unmarshalTypes = new Properties();

/**
 * Given the input object, something of an abstract syntax tree, this generates a source code file in the Python language.It has ivars, getters, setters, and serialization/deserialization methods.Warning: only partially implemented.
 * @author DMcG
     * @param pClassDescriptions String Map of class descriptions
     * @param pythonProperties special language properties
 */
    public PythonGenerator(Map<String, GeneratedClass> pClassDescriptions, Properties pythonProperties)
    {
        super(pClassDescriptions, pythonProperties);
        
        marshalTypes.setProperty("unsigned short", "unsigned_short");
        marshalTypes.setProperty("unsigned byte", "unsigned_byte");
        marshalTypes.setProperty("unsigned int", "unsigned_int");
	    marshalTypes.setProperty("unsigned long", "long"); // This is wrong; no unsigned long type in java. Fix with a BigInt or similar
        
        marshalTypes.setProperty("byte", "byte");
        marshalTypes.setProperty("short", "short");
        marshalTypes.setProperty("int", "int");
        marshalTypes.setProperty("long", "long");
        
        marshalTypes.setProperty("double", "double");
        marshalTypes.setProperty("float", "float");
        
        // Unmarshalling types
        unmarshalTypes.setProperty("unsigned short", "unsigned_short");
        unmarshalTypes.setProperty("unsigned byte", "unsigned_byte");
        unmarshalTypes.setProperty("unsigned int", "int");
        unmarshalTypes.setProperty("unsigned long", "long"); // ^^^ This is wrong--should be unsigned
        
        unmarshalTypes.setProperty("byte", "byte");
        unmarshalTypes.setProperty("short", "short");
        unmarshalTypes.setProperty("int", "int");
        unmarshalTypes.setProperty("long", "long");
        
        unmarshalTypes.setProperty("double", "double");
        unmarshalTypes.setProperty("float", "float");
    }

    @Override
    public void writeClasses()
    {
       List sortedClasses =  this.sortClasses();
       this.directory = "src/main/python";
       
       this.createDirectory();
        
       PrintWriter pw;
       
       try
       {
            // Create the new, empty file, and create printwriter object for output to it
            String outputFileName = languageProperties.getProperty("filename");
            String directoryName = languageProperties.getProperty("directory");
            System.out.println("putting network code in " + directoryName + "/" + outputFileName);
            File outputFile = new File(directoryName + "/" + outputFileName);
            outputFile.getParentFile().mkdirs();
            outputFile.createNewFile();
            pw = new PrintWriter(outputFile);
            this.writeLicense(pw);
            pw.println();
            
            pw.println("import DataInputStream");
            pw.println("import DataOutputStream");
            pw.println();
       
             
              
            System.out.println("number of classes: " + sortedClasses.size());
        Iterator it = sortedClasses.iterator();
         while(it.hasNext())
          {
           
              GeneratedClass aClass = (GeneratedClass)it.next();
              String name = aClass.getName();
              System.out.println("creating python class " + name);
              // print the source code of the class to the file
              this.writeClass(pw, aClass);
           }
         
         pw.flush();
         pw.close();
       }
        catch(IOException e)
        {
            System.err.println("problem creating class " + e);
        }
 
   } // end of writeClasses
    
    /**
     * Create custom output
     * @param printWriter output
     * @param aClass class of interest
     */
    public void writeClass(PrintWriter printWriter, GeneratedClass aClass)
    {
        printWriter.println();

        String parentClassName = aClass.getParentClass();
        if(parentClassName.equalsIgnoreCase("root"))
            parentClassName = "object";
        
        printWriter.println("class " + aClass.getName() + "( " + parentClassName + " ):");
        this.writeClassComments(printWriter, aClass);
                
        printWriter.println(INDENT + "def __init__(self):");
        printWriter.println(INDENT + INDENT + "\"\"\" Initializer for " + aClass.getName() + "\"\"\"");
        
        // If this is a subclass, call the superclass intializer
        if(!aClass.getParentClass().equalsIgnoreCase("root"))
            printWriter.println(INDENT + INDENT  + "super(" + aClass.getName() + ", self).__init__()");
        
        // Write class attributes
        List ivars = aClass.getClassAttributes();
        for(int idx = 0; idx < ivars.size(); idx++)
        {
            ClassAttribute anAttribute = (ClassAttribute)ivars.get(idx);
            
            // This attribute is a primitive. 
            if(anAttribute.getAttributeKind() == ClassAttribute.ClassAttributeType.PRIMITIVE)
            {
                
                String defaultValue = anAttribute.getDefaultValue();
                if(defaultValue == null)
                    defaultValue = "0";
                
                boolean hasComment = anAttribute.getComment() != null;
     
                printWriter.println(INDENT + INDENT  + "self." + anAttribute.getName() + " = " + defaultValue);
                if(hasComment)
                {
                    printWriter.println(INDENT + INDENT  + "\"\"\" " + anAttribute.getComment() + "\"\"\"");
                }
            } // end of primitive attribute type
            
            // This is a class
            if(anAttribute.getAttributeKind() == ClassAttribute.ClassAttributeType.CLASSREF)
            {
                String attributeType = anAttribute.getType();
                
                printWriter.println(INDENT + INDENT  + "self." + anAttribute.getName() + " = " + attributeType + "();");
                if(anAttribute.getComment() != null)
                {
                    printWriter.println(INDENT + INDENT  + "\"\"\" " + anAttribute.getComment() + "\"\"\"");
                }
            }
            
            // The attribute is a fixed list, ie an array of some type--maybe primitve, maybe a class.
            
            if( (anAttribute.getAttributeKind() == ClassAttribute.ClassAttributeType.PRIMITIVE_LIST) )
            {
                String attributeType = anAttribute.getType();
                int listLength = anAttribute.getListLength();
                String listLengthString = "" + listLength;
                
                
                if(anAttribute.getUnderlyingTypeIsPrimitive() == true)
                {
                    printWriter.print(INDENT + INDENT + "self." + anAttribute.getName() + " =  " +
                                 "[");
                    for(int arrayLength = 0; arrayLength < anAttribute.getListLength(); arrayLength++)
                    {
                        printWriter.print(" 0");
                        if(arrayLength < anAttribute.getListLength() - 1)
                            printWriter.print(",");
                    }
                    printWriter.println("]");
                }
                else
                {                    
                    printWriter.print(INDENT + INDENT + "self." + anAttribute.getName() + " =  " +
                                 "[");
                    for(int arrayLength = 0; arrayLength < anAttribute.getListLength(); arrayLength++)
                    {
                        printWriter.print(" " + attributeType + "()");
                        if(arrayLength < anAttribute.getListLength() - 1)
                            printWriter.print(",");
                    }
                    printWriter.println("]");
                }
                
                if(anAttribute.getComment() != null)
                {
                    printWriter.println(INDENT + INDENT + "\"\"\" " + anAttribute.getComment() + "\"\"\"");
                }
            }
            
            // The attribute is a variable list of some kind. 
            if( (anAttribute.getAttributeKind() == ClassAttribute.ClassAttributeType.OBJECT_LIST) )
            {
                String attributeType = anAttribute.getType();
                int listLength = anAttribute.getListLength();
                String listLengthString = "" + listLength;
                
                printWriter.println(INDENT + INDENT + "self." + anAttribute.getName() + " = []");
                
                if(anAttribute.getComment() != null)
                {
                    printWriter.println(INDENT + INDENT +"\"\"\" " + anAttribute.getComment() + "\"\"\"");
                }
            }
             
        } // end of loop through attributes
        
        // Some variables may be set to an inital value.
        List inits = aClass.getInitialValues();
        for(int idx = 0; idx < inits.size(); idx++)
        {
            InitialValue anInit = (InitialValue)inits.get(idx);
            GeneratedClass currentClass = aClass;
            boolean found = false;
        
            while(currentClass != null)
                {
                    List thisClassesAttributes = currentClass.getClassAttributes();
                    for(int jdx = 0; jdx < thisClassesAttributes.size(); jdx++)
                    {
                        ClassAttribute anAttribute = (ClassAttribute)thisClassesAttributes.get(jdx);
                        //System.out.println("--checking " + anAttribute.getName() + " against inital value " + anInitialValue.getVariable());
                        if(anInit.getVariable().equals(anAttribute.getName()))
                        {
                            found = true;
                            break;
                        }
                    }
                    currentClass = classDescriptions.get(currentClass.getParentClass());
                }
                if(!found)
                {
                    System.out.println("Could not find initial value matching attribute name for " + anInit.getVariable() + " in class " + aClass.getName());
                }
                else
                {
                    printWriter.println(INDENT + INDENT  +"self." + anInit.getVariable() + " = " + anInit.getVariableValue() );
                    printWriter.println(INDENT + INDENT + "\"\"\" initialize value \"\"\"");
                }
        } // End initialize initial values
    
    
        this.writeMarshal(printWriter, aClass);
        this.writeUnmarshal(printWriter, aClass);
        this.writeFlagMethods(printWriter, aClass);
        printWriter.println();
        printWriter.println();
        
        printWriter.flush();
    }
    
    /** The method that writes out the python marshalling code
     * @param pw PrintWriter
     * @param aClass of interest */
    public void writeMarshal(PrintWriter pw, GeneratedClass aClass)
    {
        pw.println();
        pw.println(INDENT + "def serialize(self, outputStream):" );
        pw.println(INDENT + INDENT + "\"\"\"serialize the class \"\"\"");
        
        // If this is not a top-level class, call the superclass
        String parentClassName = aClass.getParentClass();
        if(!parentClassName.equalsIgnoreCase("root"))
        {
            pw.println(INDENT + INDENT + "super( " + aClass.getName() + ", self ).serialize(outputStream)");
        }
        
        
        List attributes = aClass.getClassAttributes();
        for(int idx = 0; idx < attributes.size(); idx++)
        {
            ClassAttribute anAttribute = (ClassAttribute)attributes.get(idx);
            
            // Some fields may be declared but shouldn't be serialized
            if(anAttribute.shouldSerialize == false)
            {
                pw.println(INDENT + INDENT + "# attribute " + anAttribute.getName() + " marked as do not serialize");
                continue;
            }
            
            switch(anAttribute.getAttributeKind())
            {
                case PRIMITIVE:
                     String marshalType = marshalTypes.getProperty(anAttribute.getType());
                
                     // If we're a normal primitivetype, marshal out directly; otherwise, marshall out
                     // the list length.
                     
                     if(anAttribute.getIsDynamicListLengthField() == true)
                     {
                          ClassAttribute listAttribute = anAttribute.getDynamicListClassAttribute();
                          pw.println(INDENT + INDENT + "outputStream.write_" + marshalType + "( len(self." + listAttribute.getName() + "));");
                     }
                     else
                     {
                        pw.println(INDENT + INDENT + "outputStream.write_" + marshalType + "(self."+ anAttribute.getName() + ");");
                     }
                    pw.flush();
                    break;
                    
                case CLASSREF:
                    pw.println(INDENT + INDENT + "self." + anAttribute.getName() + ".serialize(outputStream)");
                    break;
                    
                case PRIMITIVE_LIST:
                    // Write out the method call to encode a fixed length list, aka an array.
                
                    pw.println(INDENT + INDENT + "for idx in range(0, " + anAttribute.getListLength() + "):");

                    if(anAttribute.getUnderlyingTypeIsPrimitive() == true)
                    {
                         marshalType = unmarshalTypes.getProperty(anAttribute.getType());

                        pw.println(INDENT + INDENT + INDENT +"outputStream.write_" + marshalType + "( self." + anAttribute.getName() + "[ idx ] );");
                    }
                    else if(anAttribute.listIsClass() == true) 
                    {
                        pw.println(INDENT + INDENT + INDENT+ "self." + anAttribute.getName() + "[ idx ].serialize(outputStream);");
                    }

                    pw.println();

                    break;
                    
                case OBJECT_LIST:
                    //pw.println(INDENT + INDENT + "while idx < len(" + anAttribute.getName() + "):");
                    pw.println(INDENT + INDENT + "for anObj in self." + anAttribute.getName() + ":");
                    // This is some sleaze. We're an array, but an array of what? We could be either a
                    // primitive or a class. We need to figure out which. This is done via the expedient
                    // but not very reliable way of trying to do a lookup on the type. If we don't find
                    // it in our map of primitives to marshal types, we assume it is a class.

                    marshalType = marshalTypes.getProperty(anAttribute.getType());

                    if(marshalType == null) // It's a class
                    {
                        pw.println(INDENT + INDENT + INDENT+ "anObj.serialize(outputStream)");
                    }
                    else // It's a primitive
                    {
                        pw.println(INDENT + INDENT + INDENT + "outputStream.write_" + marshalType + "( anObj )");
                    }
                    pw.println();
                    break;
            }
        }
        pw.println();

        
    }
    
    /**
     * Create custom method
     * @param printWriter output
     * @param aClass class of interest
     */
    public void writeUnmarshal(PrintWriter printWriter, GeneratedClass aClass)
    {
        printWriter.println();
        printWriter.println(INDENT + "def parse(self, inputStream):");
        printWriter.println(INDENT + INDENT + "\"\"\"\"Parse a message. This may recursively call embedded objects.\"\"\"");
        printWriter.println();
        
        // If this is not a top-level class, call the superclass
        String parentClassName = aClass.getParentClass();
        if(!parentClassName.equalsIgnoreCase("root"))
        {
            printWriter.println(INDENT + INDENT + "super( " + aClass.getName() + ", self).parse(inputStream)");
        }
        
        List attributes = aClass.getClassAttributes();
        for(int idx = 0; idx < attributes.size(); idx++)
        {
            ClassAttribute anAttribute = (ClassAttribute)attributes.get(idx);
            
            // Some fields may be declared but should not be serialized or
            // unserialized
            if(anAttribute.shouldSerialize == false)
            {
                printWriter.println(INDENT + INDENT + "# attribute " + anAttribute.getName() + " marked as do not serialize");
                continue;
            }
            
            switch(anAttribute.getAttributeKind())
            {
                case PRIMITIVE:
                    String marshalType = marshalTypes.getProperty(anAttribute.getType());
                    printWriter.println(INDENT + INDENT + "self." + anAttribute.getName() + " = inputStream.read_" + marshalType + "();");
                    break;
                    
                case CLASSREF:
                    printWriter.println(INDENT + INDENT + "self." + anAttribute.getName() + ".parse(inputStream)");
                    break;
                    
                case PRIMITIVE_LIST:
                    // Write out the method call to parse a fixed length list, aka an array.
                
                    printWriter.println(INDENT + INDENT + "self." + anAttribute.getName() + " = [0]*" + anAttribute.getListLength());
                    
                    printWriter.println(INDENT + INDENT + "for idx in range(0, " + anAttribute.getListLength() + "):");

                    if(anAttribute.getUnderlyingTypeIsPrimitive() == true)
                    {
                         marshalType = unmarshalTypes.getProperty(anAttribute.getType());
                        printWriter.println(INDENT + INDENT + INDENT + "val = inputStream.read_" + marshalType);
                        printWriter.println(INDENT + INDENT + INDENT + "self." + anAttribute.getName() + "[  idx  ] = val");
                        //pw.println(INDENT + INDENT + INDENT +"inputStream.read_" + marshalType + "( self." + anAttribute.getName() + "[ idx ] );");
                    }
                    //else if(anAttribute.listIsClass() == true) 
                    ///{
                    //    pw.println(INDENT + INDENT + INDENT+ "self." + anAttribute.getName() + "[ idx ].serialize(outputStream);");
                    //}

                    printWriter.println();
                    break;
                    
                case OBJECT_LIST:
                    printWriter.println(INDENT + INDENT + "for idx in range(0, self." + anAttribute.getCountFieldName() + "):");

                    // This is some sleaze. We're an array, but an array of what? We could be either a
                    // primitive or a class. We need to figure out which. This is done via the expedient
                    // but not very reliable way of trying to do a lookup on the type. If we don't find
                    // it in our map of primitives to marshal types, we assume it is a class.

                    marshalType = marshalTypes.getProperty(anAttribute.getType());

                    if(marshalType == null) // It's a class
                    {
                        printWriter.println(INDENT + INDENT + INDENT + "element = " + anAttribute.dynamicListClassAttribute + "()");
                        printWriter.println(INDENT + INDENT + INDENT + "element.parse(inputStream)");
                        printWriter.println(INDENT + INDENT + INDENT+ "self." + anAttribute.getName() + ".append(element)");
                    }
                    else // It's a primitive
                    {
                        printWriter.println(INDENT + INDENT + INDENT + "self." + anAttribute.getName() + ".add( inputStream.read_" + marshalType + "(  )");
                    }
                    printWriter.println();
                    
                    break;
            } // end switch  
            
        } // End loop through attributes
    }
    
    /**
     * Create custom output
     * @param printWriter output
     * @param aClass class of interest
     */
    public void writeClassComments(PrintWriter printWriter, GeneratedClass aClass)
    {
        printWriter.println(INDENT + "\"\"\"" + aClass.getClassComments() + "\"\"\"");
        printWriter.println();
    }
    
    /**
     * Some fields have integers with bit fields defined, eg an integer where 
     * bits 0-2 represent some value, while bits 3-4 represent another value, 
     * and so on. This writes accessor and mutator methods for those fields.
     * 
     * @param pw PrintWriter
     * @param aClass of interest 
     */
    public void writeFlagMethods(PrintWriter pw, GeneratedClass aClass)
    {
        List attributes = aClass.getClassAttributes();
        
        for(int idx = 0; idx < attributes.size(); idx++)
        {
            ClassAttribute anAttribute = (ClassAttribute)attributes.get(idx);
           
            
            switch(anAttribute.getAttributeKind())
            {
                
                // Anything with bitfields must be a primitive type
                case PRIMITIVE:
                    
                    List bitfields = anAttribute.bitFieldList;
   
                    for(int jdx = 0; jdx < bitfields.size(); jdx++)
                    {
                        BitField bitfield = (BitField)bitfields.get(jdx);
                        String capped = this.initialCap(bitfield.name);
                        int shiftBits = this.getBitsToShift(anAttribute, bitfield.mask);
                        
                        // write getter
                        pw.println();
                        pw.println(INDENT + "def get" + capped + "(self):");
                        if(bitfield.description != null)
                        {
                            pw.println(INDENT + INDENT + "\"\"\"" + bitfield.description + " \"\"\"");
                        }
                        
                        pw.println(INDENT + INDENT + "val = self." + bitfield.parentAttribute.getName() + " & " + bitfield.mask);
                        pw.println(INDENT + INDENT + "return val >> " + shiftBits);
                        pw.println();
                        
                        // Write the setter/mutator
                        
                        pw.println();
                        pw.println(INDENT + "def set" + capped + "(self, val):");
                        if(bitfield.description != null)
                        {
                            pw.println(INDENT + INDENT + "\"\"\"" + bitfield.description + " \"\"\"");
                        }
                        pw.println(INDENT + INDENT + "aVal = 0");
                        pw.println(INDENT + INDENT + "self." + bitfield.parentAttribute.getName() + " &= ~" + bitfield.mask);
                        pw.println(INDENT + INDENT + "val = val << " + shiftBits);
                        pw.println(INDENT + INDENT + "self." + bitfield.parentAttribute.getName() + " = self." + bitfield.parentAttribute.getName() + " | val" );
                        //pw.println(INDENT + INDENT + bitfield.parentAttribute.getName() + " = val & ~" + mask);
                        pw.println();
                    }
                    
                    break;
                    
                default:
                    bitfields = anAttribute.bitFieldList;
                    if(!bitfields.isEmpty())
                    {
                        System.out.println("Attempted to use bit flags on a non-primitive field");
                        System.out.println( "Field: " + anAttribute.getName() );
                    }
            }
        
        }
    }
    
    /**
     * Create output
     * @param printWriter output
     */
    public void writeLicense(PrintWriter printWriter)
    {
        printWriter.println("#");
        printWriter.println("#This code is licensed under the BSD software license");
        printWriter.println("# Copyright 2009-2021, MOVES Institute");
        printWriter.println("# Author: DMcG");
        printWriter.println("#");
    }
    
    
    /**
     * Python doesn't like forward-declaring classes, so a subclass must be
     * declared after its superclass.This reorders the list of classes so that
     * this is the case. This re-creates the semantic class inheritance tree
     * structure, then traverses the tree in preorder fashion to ensure that a
     * base class is written before a subclass. The implementation is a little
     * wonky in places.
     *
     * @return sorted List of classes
     */
    public List sortClasses()
    {
        List<GeneratedClass> allClasses = new ArrayList<>(classDescriptions.values());
        List<GeneratedClass> sortedClasses = new ArrayList<>();
        
        TreeNode root = new TreeNode(null);
        
        while(allClasses.size() > 0)
        {
            Iterator<GeneratedClass> li = allClasses.listIterator();
            while(li.hasNext())
            {
                GeneratedClass aClass = li.next();
                if(aClass.getParentClass().equalsIgnoreCase("root"))
                {
                    root.addClass(aClass);
                    li.remove();
                }
            }
            
           li = allClasses.listIterator();
           while(li.hasNext())
            {
                GeneratedClass aClass = li.next();
                TreeNode aNode = root.findClass(aClass.getParentClass());
                if(aNode != null)
                {
                    aNode.addClass(aClass);
                    li.remove();
                }
            }
           

        } // while all classes still has content

        // Get a sorted list
        List<TreeNode> blah = new ArrayList<>();
        root.getList(blah);
        
        Iterator<TreeNode> it = blah.iterator();
        while(it.hasNext())
        {
            TreeNode node = it.next();
            if(node.aClass != null)
                sortedClasses.add(node.aClass);
        }
                
        return sortedClasses;
    }
   
}
