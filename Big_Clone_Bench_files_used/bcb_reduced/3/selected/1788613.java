package com.enerjy.common.jdt;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;
import com.enerjy.common.EnerjyException;
import com.enerjy.common.util.IterableIterator;

class ExternalSignature {

    static final byte[] EMPTY_SIGNATURE = {};

    private Set<String> processedTypes = new HashSet<String>();

    private MessageDigest digest;

    private Comparator<IBinding> bindingComparator = new Comparator<IBinding>() {

        public int compare(IBinding o1, IBinding o2) {
            return o1.getKey().compareTo(o2.getKey());
        }
    };

    private ExternalSignature() {
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new EnerjyException("Internal error", e);
        }
    }

    static byte[] computeSignature(CompilationUnit unit) {
        ExternalSignature signature = new ExternalSignature();
        signature.update(unit);
        return signature.getBytes();
    }

    private byte[] getBytes() {
        return digest.digest();
    }

    private void update(CompilationUnit cu) {
        List<ITypeBinding> typeList = new ArrayList<ITypeBinding>();
        for (AbstractTypeDeclaration type : new IterableIterator<AbstractTypeDeclaration>(cu.types().iterator())) {
            typeList.add(type.resolveBinding());
        }
        Collections.sort(typeList, bindingComparator);
        for (ITypeBinding type : typeList) {
            update(type);
        }
    }

    private void update(ITypeBinding type) {
        update(type.getModifiers());
        update(type.getKey());
        if (processedTypes.contains(type.getKey())) {
            return;
        }
        processedTypes.add(type.getKey());
        if (null != type.getSuperclass()) {
            update(type.getSuperclass());
        }
        for (ITypeBinding superInterface : type.getInterfaces()) {
            update(superInterface);
        }
        IMethodBinding[] methods = type.getDeclaredMethods();
        Arrays.sort(methods, bindingComparator);
        for (IMethodBinding method : methods) {
            update(method);
        }
        IVariableBinding[] fields = type.getDeclaredFields();
        Arrays.sort(fields, bindingComparator);
        for (IVariableBinding field : fields) {
            update(field);
        }
        ITypeBinding[] types = type.getDeclaredTypes();
        Arrays.sort(types, bindingComparator);
        for (ITypeBinding memberType : types) {
            update(memberType);
        }
    }

    private void update(IMethodBinding method) {
        if (Modifier.isPrivate(method.getModifiers())) {
            return;
        }
        update(method.getModifiers());
        update(method.getKey());
        for (ITypeBinding exception : method.getExceptionTypes()) {
            update(exception.getKey());
        }
    }

    private void update(IVariableBinding field) {
        if (Modifier.isPrivate(field.getModifiers())) {
            return;
        }
        update(field.getModifiers());
        update(field.getKey());
        if (null != field.getConstantValue()) {
            update(field.getConstantValue().toString());
        }
    }

    private void update(int intValue) {
        digest.update(String.valueOf(intValue).getBytes());
    }

    private void update(String stringValue) {
        digest.update(stringValue.getBytes());
    }
}
