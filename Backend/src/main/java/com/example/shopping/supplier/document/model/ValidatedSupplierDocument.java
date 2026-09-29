package com.example.shopping.supplier.document.model;

public record ValidatedSupplierDocument(
    String filename, 
    String contentType, 
    byte[] bytes) 
    { }
