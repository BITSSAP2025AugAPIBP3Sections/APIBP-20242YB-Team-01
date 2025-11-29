import React, { useState } from "react";
import MasterList from "./MasterList";
import ExplorePage from "../pages/ExplorePage";
import DashboardPage from "../pages/DashboardPage";
import MyAuctionPage from "../pages/MyAuctionPage";
import PaymentPage from "../pages/PaymentPage";
import SettingsPage from "../pages/SettingsPage";
import HelpAndSupportPage from "../pages/HelpAndSupportPage";
import styles from "../css/SideMenu.module.css";
import { useSection } from "../context/SectionContext";

export default function SideMenu() {
  const { section: selectedSection, setSection: setSelectedSection } = useSection();
  const [menuOpen, setMenuOpen] = useState(false);

  const renderDetail = () => {
    switch (selectedSection) {
      case "explore":
        return <ExplorePage />;
      case "dashboard":
        return <DashboardPage />;
      case "myauction":
        return <MyAuctionPage />;
      case "payment":
        return <PaymentPage />;       // ← Re-added so setSection("payment") works
      case "helpandsupport":
        return <HelpAndSupportPage />;
      default:
        return <ExplorePage />;
    }
  };

  return (
      <div className={`${styles.sidemenu} ${menuOpen ? styles.open : ""}`}>
        <aside className={styles.sidebar}>
          <MasterList
              selectedSection={selectedSection}
              onSelectSection={(key) => {
                setSelectedSection(key);
                setMenuOpen(false);
              }}
          />
        </aside>

        <main className={styles.content} onClick={() => setMenuOpen(false)}>
          {renderDetail()}
        </main>

        <button
            className={styles.profileToggle}
            onClick={() => setMenuOpen((o) => !o)}
            aria-label={menuOpen ? "Close menu" : "Open menu"}
        >
          <span className={styles.avatar}>👤</span>
        </button>
      </div>
  );
}
 