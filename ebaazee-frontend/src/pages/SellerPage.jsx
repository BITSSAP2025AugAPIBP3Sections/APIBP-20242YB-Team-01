import React, { useState, useEffect } from 'react';
import styles from '../css/SellerPage.module.css';

export default function SellerPage() {
  const [categories, setCategories] = useState([]);
  const [form, setForm] = useState({
    title: '',
    description: '',
    category: '',
    endTime: '',
    minBid: '',
    maxBid: '',
  });

  useEffect(() => {
    const fetchCategories = async () => {
      try {
        const token = localStorage.getItem('token');
        const response = await fetch('http://localhost:8080/api/categories/v1', {
          headers: {
            'Authorization': `Bearer ${token}`,
          },
        });

        if (!response.ok) {
          throw new Error('Failed to load categories');
        }

        const data = await response.json();
        setCategories(data);
      } catch (err) {
        console.error(err);
        alert('Error loading categories. Please make sure you are logged in.');
      }
    };

    fetchCategories();
  }, []);

  const handleChange = e => {
    const { name, value } = e.target;
    setForm(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async e => {
    e.preventDefault();

    try {
      const token = localStorage.getItem('token');
      const resp = await fetch('http://localhost:8080/api/products/v1', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          name: form.title,
          description: form.description,
          category: form.category,
          endTime: form.endTime,
          minBid: parseFloat(form.minBid),
          maxBid: parseFloat(form.maxBid),
        }),
      });

      if (!resp.ok) {
        const msg = await resp.text();
        throw new Error(msg || 'Network error');
      }

      alert('Product created!');
      setForm({
        title: '',
        description: '',
        category: '',
        endTime: '',
        minBid: '',
        maxBid: '',
      });
    } catch (err) {
      console.error(err);
      alert(`Error: ${err.message}`);
    }
  };

  return (
    <div className={styles.root}>
      <div className={styles.card}>
        <h1 className={styles.heading}>Create New Auction Listing</h1>
        <form className={styles.form} onSubmit={handleSubmit}>

          {/* Title */}
          <div className={styles.field}>
            <label htmlFor="title">Title</label>
            <input
              id="title"
              name="title"
              type="text"
              value={form.title}
              onChange={handleChange}
              placeholder="e.g. Vintage Lamp"
              required
            />
          </div>

          {/* Description */}
          <div className={styles.field}>
            <label htmlFor="description">Description</label>
            <textarea
              id="description"
              name="description"
              rows="4"
              value={form.description}
              onChange={handleChange}
              placeholder="Write a brief description of your item..."
              required
            />
          </div>

          {/* Category */}
          <div className={styles.field}>
            <label htmlFor="category">Category</label>
            <select
              id="category"
              name="category"
              value={form.category}
              onChange={handleChange}
              required
            >
              <option value="">— Select a category —</option>
              {categories.map(cat => (
                <option key={cat.id} value={cat.name}>
                  {cat.name}
                </option>
              ))}
            </select>
          </div>

          {/* End Time */}
          <div className={styles.field}>
            <label htmlFor="endTime">End Time</label>
            <input
              id="endTime"
              name="endTime"
              type="datetime-local"
              value={form.endTime}
              onChange={handleChange}
              required
            />
          </div>

          {/* Bid range */}
          <div className={styles.fieldGroup}>
            <div className={styles.field}>
              <label htmlFor="minBid">Minimum Bid ($)</label>
              <input
                id="minBid"
                name="minBid"
                type="number"
                min="0"
                step="0.01"
                value={form.minBid}
                onChange={handleChange}
                required
              />
            </div>
            <div className={styles.field}>
              <label htmlFor="maxBid">Maximum Bid ($)</label>
              <input
                id="maxBid"
                name="maxBid"
                type="number"
                min="0"
                step="0.01"
                value={form.maxBid}
                onChange={handleChange}
                required
              />
            </div>
          </div>

          <button type="submit" className={styles.submitButton}>
            Create Listing
          </button>
        </form>
      </div>
    </div>
  );
}
