import { createContext, useState, useContext } from 'react';

const SectionContext = createContext();
export function SectionProvider({ children }) {
  const [section, setSection] = useState('explore');
  return (
    <SectionContext.Provider value={{ section, setSection }}>
      {children}
    </SectionContext.Provider>
  );
}
export function useSection() {
  return useContext(SectionContext);
}
