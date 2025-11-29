// src/pages/HelpAndSupportPage.jsx
import React, { useState } from 'react';
import * as FaIcons from 'react-icons/fa';
import helpTopics from '../data/helpTopics.json';
import styles from '../css/HelpAndSupportPage.module.css';

export default function HelpAndSupportPage() {
  const [query, setQuery]     = useState('');
  const [openIndex, setOpenIndex] = useState(null);

  // filter topics by query
  const filtered = helpTopics.filter(t =>
    t.title.toLowerCase().includes(query.toLowerCase())
  );

  const toggle = idx =>
    setOpenIndex(openIndex === idx ? null : idx);

  return (
    <div className={styles.root}>
      <div className={styles.topBar}>
        <input
          type="text"
          value={query}
          onChange={e => setQuery(e.target.value)}
          placeholder="Search Help Topics"
          className={styles.searchInput}
        />
      </div>

      <div className={styles.section}>
        <h2 className={styles.sectionTitle}>Help & Support</h2>
        <ul className={styles.list}>
          {filtered.map((t, i) => {
            const isOpen = i === openIndex;
            // dynamically pick the icon component
            const Icon = FaIcons[t.icon] || FaIcons.FaQuestionCircle;

            return (
              <li key={i} className={styles.item}>
                <button
                  onClick={() => toggle(i)}
                  className={styles.questionRow}
                >
                  <div className={styles.iconWrapper}>
                    <Icon size={20} />
                  </div>
                  <span className={styles.questionText}>
                    {t.title}
                  </span>
                  <div className={styles.chevron}>
                    {isOpen
                      ? <FaIcons.FaChevronUp />
                      : <FaIcons.FaChevronDown />}
                  </div>
                </button>
                <div
                  className={`${styles.answer} ${
                    isOpen ? styles.open : ''
                  }`}
                >
                  {t.content}
                </div>
              </li>
            );
          })}
        </ul>
      </div>
    </div>
  );
}
