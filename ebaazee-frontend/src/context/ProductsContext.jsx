// src/context/ProductsContext.jsx
import React, { createContext, useState, useContext, useEffect } from 'react';
import listingData from '../data/listings.json';

const ProductsContext = createContext();

export function ProductsProvider({ children }) {
  // initialize from your mock JSON
  const [products, setProducts] = useState([]);

  useEffect(() => {
    setProducts(listingData.listings);
  }, []);

  const updateStatus = (id, newStatus) => {
    setProducts(ps =>
      ps.map(p => (p.id === id ? { ...p, status: newStatus } : p))
    );
  };

  return (
    <ProductsContext.Provider value={{ products, updateStatus }}>
      {children}
    </ProductsContext.Provider>
  );
}

export function useProducts() {
  return useContext(ProductsContext);
}
