import React, {useState,useEffect} from 'react';
import data from '../data/DashboardData.json';
import styles from '../css/DashboardPage.module.css';
import userData from '../data/UserProfile.json';
import defaultAvatars from '../data/defaultAvatars.json';

export default function DashboardPage() {
  const [firstName, setFirstName] = useState('');
  const [avatar, setAvatar] = useState(null);
  const [bids, setBids] = useState([]);
  const [pagination, setPagination] = useState({ current: 1, total: 1 }); // Placeholder
  const [stats, setStats] = useState([]); // Placeholder


    useEffect(() => {
      const token = localStorage.getItem('token');
      const id = localStorage.getItem('id');
      if (!token) return;
      fetch(`http://localhost:8080/api/auth/v1/users/${id}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })
      .then(res => {
        if (!res.ok) throw new Error('Network error');
        return res.json();
      })
      .then(data => {
        setFirstName(data.fullName);
      })
      .catch(() => console.log("error"));
    }, []);
  
  useEffect(() => {
    if (!avatar) {
      const randomIndex = Math.floor(Math.random() * defaultAvatars.length);
      setAvatar(defaultAvatars[randomIndex]);
    }
  }, [avatar]);

  useEffect(() => {
    const userId = localStorage.getItem('id');
    const token = localStorage.getItem('token');
    
    if (!userId || !token) {
      console.error('User ID or token not found');
      return;
    }

    fetch(`http://localhost:8080/api/bids/v1/users/${userId}/summary`, {
      credentials: 'include',
      method: 'GET',
      headers: {
        'Accept': 'application/json',
        'Authorization': `Bearer ${token}`
      }
    })
      .then(res => {
        if (!res.ok) throw new Error('Failed to fetch bidding summary');
        return res.json();
      })
      .then(data => {
        setBids(data);
        console.log('Bidding summary:', data);
        // Optionally calculate stats or pagination
      })
      .catch(err => {
        console.error('Error fetching bidding summary:', err);
      });
  }, []);

  return (
    <div className={styles.root}>
      {/* Greeting */}
      <header className={styles.header}>
        {avatar && <img src={avatar} alt="Avatar" className={styles.avatar} />}
        <div>
          <h1 className={styles.greeting}>Hi, {firstName}</h1>
          <p className={styles.subtitle}>
            You had participated in {bids.length} auctions. Start your auction today.
          </p>
        </div>
      </header>

      {/* Stats cards (placeholder) */}
      <div className={styles.statsGrid}>
        {stats.map(s => (
          <div
            key={s.key}
            className={styles.statCard}
            style={{ backgroundColor: s.color }}
          >
            <div className={styles.statLabel}>{s.label}</div>
            <div className={styles.statValue}>{s.value}</div>
          </div>
        ))}
      </div>

      {/* Bidding summary table */}
      <section className={styles.tableSection}>
        <h2 className={styles.tableTitle}>Bidding Summary</h2>
        <div className={styles.tableWrapper}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>Auction ID</th>
                <th>Product Name</th>
                <th>Amount</th>
                <th>Status</th>
{/*                 <th>Auction Date</th> */}
                <th>Bid Date</th>
                <th>Bid Time</th>
                <th>Auction End Date</th>
                <th>Auction End Time</th>
              </tr>
            </thead>
            <tbody>
              {bids.map(row => (
                <tr key={row.id}>
                  <td>{row.id}</td>
                  <td>{row.productName}</td>
                  <td>${row.amount.toLocaleString()}</td>
                  <td>
                    <span
                      className={
                        row.status === 'Winning'
                          ? styles.statusWin
                          : styles.statusCancel
                      }
                    >
                      {row.status}
                    </span>
                  </td>
                  <td>
                    {row.bidTime && (
                      <>
                        <div>{row.bidTime.split('T')[0]}</div> {/* Date */}
                      </>
                    )}
                  </td>
                  <td>
                    {row.bidTime && (
                      <>
                        <div>{row.bidTime.split('T')[1].split('.')[0]}</div> {/* Time */}
                      </>
                    )}
                  </td>
                 <td>
                   {row.endTime && (
                     <>
                       <div>{row.endTime.split('T')[0]}</div> {/* Date */}
                     </>
                   )}
                 </td>
                 <td>
                   {row.endTime && (
                     <>
                       <div>{row.endTime.split('T')[1].split('.')[0]}</div> {/* Time */}
                     </>
                   )}
                 </td>

                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Pagination placeholder */}
        <div className={styles.pagination}>
          {Array.from({ length: pagination.total }, (_, i) => (
            <button
              key={i + 1}
              className={
                i + 1 === pagination.current
                  ? styles.pageActive
                  : styles.pageButton
              }
            >
              {i + 1}
            </button>
          ))}
          <button className={styles.pageButton}>&rarr;</button>
        </div>
      </section>
    </div>
  );
}
