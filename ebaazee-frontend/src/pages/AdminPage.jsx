import React, { useEffect, useState } from 'react';
import axios from 'axios';
import styles from '../css/AdminPage.module.css';

const AdminPage = () => {
  const [categories, setCategories] = useState([]);
  const [products, setProducts] = useState([]);
  const [filteredProducts, setFilteredProducts] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState('');
  const [selectedStatus, setSelectedStatus] = useState('');
  const [showCategoryModal, setShowCategoryModal] = useState(false);
  const [newCategoryName, setNewCategoryName] = useState('');

  const token = localStorage.getItem('token');

  useEffect(() => {
    fetchCategories();
    fetchProducts();
  }, []);

  useEffect(() => {
    applyFilters();
  }, [products, selectedCategory, selectedStatus]);

  const fetchCategories = async () => {
    try {
      const res = await axios.get('http://localhost:8080/api/categories/v1', {
        headers: { Authorization: `Bearer ${token}` },
      });
      // API returns array of category objects with id and name
      setCategories(res.data);
    } catch (error) {
      console.error('Error fetching categories:', error);
    }
  };

  const handleCreateCategory = async () => {
    if (!newCategoryName.trim()) {
      alert('Please enter a category name');
      return;
    }
    
    try {
      await axios.post(
        'http://localhost:8080/api/categories/v1',
        { name: newCategoryName },
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );
      setNewCategoryName('');
      setShowCategoryModal(false);
      fetchCategories();
      alert('Category created successfully!');
    } catch (error) {
      console.error('Error creating category:', error);
      alert('Failed to create category. Please try again.');
    }
  };

  const fetchProducts = async () => {
    try {
      const res = await axios.get('http://localhost:8080/api/products/v1', {
        headers: { Authorization: `Bearer ${token}` },
      });
      setProducts(res.data);
    } catch (error) {
      console.error('Error fetching products:', error);
    }
  };

  // Determine product status based on isSold and isFrozen
  const getStatus = (product) => {
    if (product.sold && product.frozen) return 'SOLD';
    if (!product.sold && product.frozen) return 'FROZEN';
    if (!product.sold && !product.frozen) return 'ACTIVE';
    return 'UNKNOWN';
  };

  const applyFilters = () => {
    let filtered = [...products];
    if (selectedCategory) {
      // Filter by category name
      filtered = filtered.filter((p) => p.category === selectedCategory);
    }
    if (selectedStatus) {
      filtered = filtered.filter(
          (p) => getStatus(p) === selectedStatus.toUpperCase()
      );
    }
    setFilteredProducts(filtered);
  };

  const handleFreeze = async (productId) => {
    try {
      await axios.put(
          `http://localhost:8080/api/products/v1/${productId}`,
          { frozen: true },
          {
            headers: { Authorization: `Bearer ${token}` },
          }
      );
      fetchProducts();
    } catch (error) {
      console.error('Error freezing product:', error);
    }
  };

  const downloadReport = async () => {
    try {
      console.log('Starting download report...');
      const res = await axios.post(
          'http://localhost:8080/api/analytics/graphql',
          {
            query: `query { downloadProductReport { filename base64Content } }`
          },
          {
            headers: {
              Authorization: `Bearer ${token}`,
              'Content-Type': 'application/json',
            },
          }
      );

      console.log('Response:', res.data);

      // Check if response has the expected structure
      if (!res.data || !res.data.data || !res.data.data.downloadProductReport) {
        throw new Error('Invalid response structure');
      }

      const { filename, base64Content } = res.data.data.downloadProductReport;
      
      console.log('Filename:', filename);
      console.log('Base64 content length:', base64Content.length);
      
      // Convert base64 to blob
      const byteCharacters = atob(base64Content);
      const byteNumbers = new Array(byteCharacters.length);
      for (let i = 0; i < byteCharacters.length; i++) {
        byteNumbers[i] = byteCharacters.charCodeAt(i);
      }
      const byteArray = new Uint8Array(byteNumbers);
      const blob = new Blob([byteArray], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
      
      console.log('Blob created, size:', blob.size);
      
      // Create download link
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      
      console.log('Download triggered successfully');
      alert('Report downloaded successfully!');
    } catch (error) {
      console.error('Error downloading report:', error);
      console.error('Error details:', error.response?.data);
      alert(`Failed to download report: ${error.message || 'Please try again.'}`);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    window.location.href = '/login';
  };

  const statusColors = {
    ACTIVE: '#4caf50', // Green
    FROZEN: '#ff9800', // Orange
    SOLD: '#9e9e9e',   // Gray
    UNKNOWN: '#f44336' // Red
  };

  return (
      <div className={styles.adminContainer}>
        <header className={styles.adminHeader}>
          <h1>Admin Page</h1>
          <div className={styles.headerActions}>
            <button className={`${styles.btn} ${styles.reportBtn}`} onClick={() => setShowCategoryModal(true)}>
              Create Category
            </button>
            <button className={`${styles.btn} ${styles.reportBtn}`} onClick={downloadReport}>
              Download Report
            </button>
            <button className={`${styles.btn} ${styles.logoutBtn}`} onClick={handleLogout}>
              Logout
            </button>
          </div>
        </header>

        <section className={styles.filters}>
          <div className={styles.filterGroup}>
            <label htmlFor="category-select">Filter by Category:</label>
            <select
                id="category-select"
                value={selectedCategory}
                onChange={(e) => setSelectedCategory(e.target.value)}
            >
              <option value="">All</option>
              {categories.map((cat) => (
                  <option key={cat.id} value={cat.name}>
                    {cat.name}
                  </option>
              ))}
            </select>
          </div>

          <div className={styles.filterGroup}>
            <label htmlFor="status-select">Filter by Status:</label>
            <select
                id="status-select"
                value={selectedStatus}
                onChange={(e) => setSelectedStatus(e.target.value)}
            >
              <option value="">All</option>
              <option value="ACTIVE">Active</option>
              <option value="FROZEN">Frozen</option>
              <option value="SOLD">Sold</option>
            </select>
          </div>
        </section>

        <table className={styles.productTable}>
          <thead>
          <tr>
            <th>Id</th>
            <th>Title</th>
            <th>Description</th>
            <th>Category</th>
            <th>Seller</th>
            <th>Current Bid</th>
            <th>Status</th>
            <th>End Time</th>
            <th>Actions</th>
          </tr>
          </thead>
          <tbody>
          {filteredProducts.length === 0 ? (
              <tr>
                <td colSpan="9" style={{textAlign: 'center', padding: '20px'}}>
                  No products found.
                </td>
              </tr>
          ) : (
              filteredProducts.map((product) => (
                  <tr key={product.id}>
                    <td>{product.id}</td>
                    <td>{product.name || product.description || 'N/A'}</td>
                    <td>{product.description}</td>
                    <td>{product.category.replace('_', ' ')}</td>
                    <td>
                      {product.seller
                          ? `${product.seller.firstName} ${product.seller.lastName}`
                          : 'N/A'}
                    </td>
                    <td>{product.currentBid?.toLocaleString() || 0}</td>
                    <td>
                  <span
                      className={styles.statusBadge}
                      style={{backgroundColor: statusColors[getStatus(product)]}}
                  >
                    {getStatus(product)}
                  </span>
                    </td>
                    <td>{new Date(product.endTime).toLocaleString()}</td>
                    <td>
                      {getStatus(product) === 'ACTIVE' ? (
                          <button
                              className={styles.freezeBtn}
                              onClick={() => handleFreeze(product.id)}
                          >
                            Freeze
                          </button>
                      ) : (
                          <span style={{color: '#888'}}>N/A</span>
                      )}
                    </td>
                  </tr>
              ))
          )}
          </tbody>
        </table>

        {/* Create Category Modal */}
        {showCategoryModal && (
          <div className={styles.modal}>
            <div className={styles.modalContent}>
              <h2>Create New Category</h2>
              <input
                type="text"
                placeholder="Enter category name"
                value={newCategoryName}
                onChange={(e) => setNewCategoryName(e.target.value)}
                className={styles.categoryInput}
              />
              <div className={styles.modalActions}>
                <button
                  className={`${styles.btn} ${styles.createBtn}`}
                  onClick={handleCreateCategory}
                >
                  Create
                </button>
                <button
                  className={`${styles.btn} ${styles.cancelBtn}`}
                  onClick={() => {
                    setShowCategoryModal(false);
                    setNewCategoryName('');
                  }}
                >
                  Cancel
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
  );
};

export default AdminPage;
