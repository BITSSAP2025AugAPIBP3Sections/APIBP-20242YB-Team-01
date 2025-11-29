import React, { useState, useRef, useEffect } from "react";
import filterOptions from "../data/filters.json";
import styles from "../css/ExplorePage.module.css";
import listStyles from "../css/ListingSection.module.css";
import { useSection } from "../context/SectionContext";
import Wallet from "../components/Wallet";

import appliances from "../assets/categories/appliances.jpg";
import automotive from "../assets/categories/automotive.jpg";
import beauty from "../assets/categories/beauty.jpg";
import books from "../assets/categories/books.jpg";
import electronics from "../assets/categories/electronics.jpg";
import fashion from "../assets/categories/fashion.jpg";
import garden from "../assets/categories/garden.jpg";
import music from "../assets/categories/music.jpg";
import sports from "../assets/categories/sports.jpg";
import toys from "../assets/categories/toys.jpg";
import defaultImg from "../assets/categories/all.jpg";

// Countdown timer for product endTime
function CountdownTimer({ endTime }) {
  const [timeLeft, setTimeLeft] = useState(getTimeLeft());

  function getTimeLeft() {
    const end = new Date(endTime).getTime();
    const now = Date.now();
    const diff = Math.max(0, end - now);
    return {
      days: Math.floor(diff / (1000 * 60 * 60 * 24)),
      hours: Math.floor((diff / (1000 * 60 * 60)) % 24),
      min: Math.floor((diff / (1000 * 60)) % 60),
      sec: Math.floor((diff / 1000) % 60),
      finished: diff === 0,
    };
  }

  useEffect(() => {
    const timer = setInterval(() => setTimeLeft(getTimeLeft()), 1000);
    return () => clearInterval(timer);
    // eslint-disable-next-line
  }, [endTime]);

  return (
    <>
      <div className={styles.timeSegment}>
        <span>{String(timeLeft.days).padStart(2, "0")}</span>
        <small>Days</small>
      </div>
      <div className={styles.timeSegment}>
        <span>{String(timeLeft.hours).padStart(2, "0")}</span>
        <small>Hours</small>
      </div>
      <div className={styles.timeSegment}>
        <span>{String(timeLeft.min).padStart(2, "0")}</span>
        <small>Min</small>
      </div>
      <div className={styles.timeSegment}>
        <span>{String(timeLeft.sec).padStart(2, "0")}</span>
        <small>Sec</small>
      </div>
    </>
  );
}

export default function ExplorePage() {
  // ─── Carousel static data ───────────────────────────────────────────
  const [carouselItems] = useState([
    {
      id: 1,
      title: "Welcome to Auction",
      text: "Bid on exciting products!",
      buttonText: "Explore Now",
    },
    {
      id: 2,
      title: "Exclusive Deals",
      text: "Grab the best offers",
      buttonText: "Shop Deals",
    },
  ]);

  // ─── Categories & listings ─────────────────────────────────────────
  const [categories, setCategories] = useState([
    { value: "ALL", label: "All", image: "/images/default.png" },
  ]);
  const [listings, setListings] = useState([]);
  const [searchText, setSearchText] = useState("");
  const [activeCategory, setActiveCategory] = useState("ALL");
  const [sales, setSales] = useState("all");

  // ─── Modal state ────────────────────────────────────────────────────
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [bidAmount, setBidAmount] = useState("");
  const [feedback, setFeedback] = useState("");

  // ─── Fetch bid status for modal ─────────────────────────────────────
  const [bidStatus, setBidStatus] = useState({
    totalBidders: 0,
    averageBidAmount: 0,
    maxBidAmount: 0,
    hasUserBid: false,
    userBidAmount: 0,
  });

  // ─── Profile (for “Hi, FirstName”) ─────────────────────────────────
  const [firstName, setFirstName] = useState("");

  // ─── Carousel state ─────────────────────────────────────────────────
  const [current, setCurrent] = useState(0);
  const scrollRef = useRef(null);
  const didMountMain = useRef(false);

  // ─── Category carousel state ────────────────────────────────────────
  const [catIndex, setCatIndex] = useState(0);
  const catRef = useRef(null);
  const didMountCat = useRef(false);

  // ─── Context ────────────────────────────────────────────────────────
  const { setSection } = useSection();

  // ─── Category ↔ image mapping ────────────────────────────────────────
  const categoryImageMap = {
    HOME_APPLIANCES: appliances,
    AUTOMOTIVE: automotive,
    BEAUTY: beauty,
    BOOKS: books,
    ELECTRONICS: electronics,
    FASHION: fashion,
    GARDEN: garden,
    MUSIC: music,
    SPORTS: sports,
    TOYS: toys,
    DEFAULT: defaultImg,
  };
  const getCategoryImage = (cat) =>
    categoryImageMap[cat.toUpperCase()] || categoryImageMap.DEFAULT;

  // ─── Fetch User Profile on mount ────────────────────────────────────
  useEffect(() => {
    const token = localStorage.getItem("token");
    const id = localStorage.getItem('id');
    console.log("User ID from localStorage:", id);
    fetch(`http://localhost:8080/api/auth/v1/users/${id}`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((res) => {
        if (!res.ok) throw new Error("Unauthorized");
        console.log(res);
        return res.json();
      })
      .then((data) => {
        console.log(data);
        setFirstName(data.fullName);
      })
      .catch((err) => {
        console.error("Error fetching profile:", err);
      });
  }, []);

  // ─── Fetch categories (prepend “All”) ───────────────────────────────
  useEffect(() => {
    const token = localStorage.getItem("token");

    fetch("http://localhost:8080/api/categories/v1", {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((res) => {
        if (!res.ok) throw new Error("Unauthorized");
        return res.json();
      })
      .then((data) => {
        const mapped = data.map((cat) => ({
          value: cat.name,   // <-- NEW (use name from object)
          label: cat.name.charAt(0).toUpperCase() + cat.name.slice(1).toLowerCase(),
          image: getCategoryImage(cat.name),   // <-- NEW (pass name)
        }));

        setCategories([
          {
            value: "ALL",
            label: "All",
            image: getCategoryImage("DEFAULT"),
          },
          ...mapped,
        ]);

        setActiveCategory("ALL");
      })
      .catch((err) => console.error("Error fetching categories:", err));
  }, []);


  // ─── Fetch products (filtered by category) ──────────────────────────
  useEffect(() => {
    const token = localStorage.getItem("token");
    const url =
      activeCategory && activeCategory !== "ALL"
        ? `http://localhost:8080/api/products/v1/category/${activeCategory}`
        : `http://localhost:8080/api/products/v1`;

    fetch(url, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((res) => {
        if (!res.ok) throw new Error("Unauthorized");
        return res.json();
      })
      .then((data) => {
        const mapped = data.map((item) => ({
          ...item,
          productId: item.productId || item.id,
          productName: item.productName || item.name || "Unnamed",
        }));
        setListings(mapped);
      })
      .catch((err) => console.error("Error fetching products:", err));
  }, [activeCategory]);

  // ─── Main carousel scroll effect ───────────────────────────────────
  useEffect(() => {
    const container = scrollRef.current;
    const slide = container?.children[current];
    if (!didMountMain.current) {
      didMountMain.current = true;
      return;
    }
    if (container && slide) {
      container.scrollTo({ left: slide.offsetLeft, behavior: "smooth" });
    }
  }, [current]);
  const prev = () => setCurrent((i) => Math.max(0, i - 1));
  const next = () =>
    setCurrent((i) => Math.min(carouselItems.length - 1, i + 1));

  // ─── Category carousel scroll (on click) ──────────────────────────
  const scrollToCategory = (i) => {
    const container = catRef.current;
    const node = container?.children[i];
    if (!didMountCat.current) {
      didMountCat.current = true;
      return;
    }
    if (node) {
      node.scrollIntoView({
        behavior: "smooth",
        inline: "center",
        block: "nearest",
      });
    }
  };
  const prevCat = () =>
    setCatIndex((i) => {
      const ni = Math.max(0, i - 1);
      scrollToCategory(ni);
      return ni;
    });
  const nextCat = () =>
    setCatIndex((i) => {
      const ni = Math.min(categories.length - 1, i + 1);
      scrollToCategory(ni);
      return ni;
    });

  // ─── Modal open/close ───────────────────────────────────────────────
  const openBidModal = (product) => {
    setSelectedProduct(product);
    setBidAmount("");
    setFeedback("");
    setModalOpen(true);
    fetchBidStatus(product.productId);
  };
  const closeBidModal = () => setModalOpen(false);

  // ─── Fetch “/api/bids/status/{productId}” ──────────────────────────
  // const fetchBidStatus = async (productId) => {
  //   const token = localStorage.getItem("token");
  //   try {
  //     const res = await fetch(
  //         `http://localhost:8080/api/bids/v1/products/${productId}`,
  //         {
  //           headers: { Authorization: `Bearer ${token}` },
  //         }
  //     );
  //     if (!res.ok) {
  //       throw new Error("Failed to fetch bid status");
  //     }
  //     const data = await res.json();
  //     setBidStatus({
  //       totalBidders: data.totalBidders,
  //       averageBidAmount: data.averageBidAmount,
  //       maxBidAmount: data.maxBidAmount,
  //       hasUserBid: data.hasUserBid,
  //       userBidAmount: data.userBidAmount,
  //     });
  //   } catch (err) {
  //     console.error("Error fetching bid status:", err);
  //     setBidStatus({
  //       totalBidders: 0,
  //       averageBidAmount: 0,
  //       maxBidAmount: 0,
  //       hasUserBid: false,
  //       userBidAmount: 0,
  //     });
  //   }
  // };
  const fetchBidStatus = async (productId) => {
    const token = localStorage.getItem("token");

    try {
      const res = await fetch(
        `http://localhost:8080/api/bids/v1/products/${productId}`,
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );

      if (!res.ok) {
        throw new Error("Failed to fetch bid status");
      }

      const bids = await res.json();

      // Get user ID from localStorage (convert to number for comparison)
      const userId = parseInt(localStorage.getItem("id"));

      // Get unique bidders (multiple bids from same user should count as one bidder)
      const uniqueBidders = new Set(bids.map((b) => b.bidderId));
      const totalBidders = uniqueBidders.size;

      // Calculate max bid amount
      const maxBidAmount =
        bids.length > 0
          ? Math.max(...bids.map((b) => b.amount))
          : 0;

      // Calculate average bid amount
      const averageBidAmount =
        bids.length > 0
          ? bids.reduce((sum, b) => sum + b.amount, 0) / bids.length
          : 0;

      // Check if current user has bid (compare as numbers)
      const userBid = bids.find((b) => b.bidderId === userId);

      const hasUserBid = !!userBid;
      const userBidAmount = userBid ? userBid.amount : 0;

      // Set state
      setBidStatus({
        totalBidders,
        averageBidAmount,
        maxBidAmount,
        hasUserBid,
        userBidAmount,
      });

    } catch (err) {
      console.error("Error fetching bid status:", err);
      setBidStatus({
        totalBidders: 0,
        averageBidAmount: 0,
        maxBidAmount: 0,
        hasUserBid: false,
        userBidAmount: 0,
      });
    }
  };


  const handleProceedToPayment = async () => {
    if (!selectedProduct) return;

    // Check if user has already bid
    if (bidStatus.hasUserBid) {
      setFeedback("⚠️ You have already placed a bid on this product.");
      return;
    }

    const amt = parseFloat(bidAmount);
    
    // Validation checks
    if (isNaN(amt) || amt <= 0) {
      setFeedback("⚠️ Please enter a valid bid amount.");
      return;
    }

    if (amt <= selectedProduct.currentBid) {
      setFeedback(`⚠️ Your bid must be greater than the current bid of $${selectedProduct.currentBid.toFixed(2)}.`);
      return;
    }
    
    if (amt < selectedProduct.minBid) {
      setFeedback(`⚠️ Your bid must be at least $${selectedProduct.minBid.toFixed(2)}.`);
      return;
    }
    
    if (amt > selectedProduct.maxBid) {
      setFeedback(`⚠️ Your bid cannot exceed $${selectedProduct.maxBid.toFixed(2)}.`);
      return;
    }

    const token = localStorage.getItem("token");
    setFeedback(""); // Clear previous feedback
    
    try {
      // Place bid using the auction service endpoint
      const response = await fetch(
        `http://localhost:8080/api/bids/v1`,
        {
          method: "POST",
          headers: { 
            "Authorization": `Bearer ${token}`,
            "Content-Type": "application/json"
          },
          body: JSON.stringify({
            amount: amt,
            productId: selectedProduct.productId
          })
        }
      );

      const data = await response.json();

      if (response.ok) {
        // Success - show the message from the response
        alert(`✅ ${data.message || "Bid placed successfully!"}\nBid ID: ${data.bidId}\nAmount: $${data.amount}`);
        closeBidModal();
        // Refresh the products to show updated bid
        const url =
          activeCategory && activeCategory !== "ALL"
            ? `http://localhost:8080/api/products/v1/category/${activeCategory}`
            : `http://localhost:8080/api/products/v1`;
        
        fetch(url, {
          headers: { Authorization: `Bearer ${token}` },
        })
          .then((res) => res.json())
          .then((data) => {
            const mapped = data.map((item) => ({
              ...item,
              productId: item.productId || item.id,
              productName: item.productName || item.name || "Unnamed",
            }));
            setListings(mapped);
          })
          .catch((err) => console.error("Error refreshing products:", err));
      } else {
        // Error - show the error message from the response
        const errorMsg = data.errorMessage || data.error || "Failed to place bid";
        setFeedback(`❌ ${errorMsg}${data.reason ? ` - ${data.reason}` : ""}`);
      }

    } catch (err) {
      console.error("Error placing bid:", err);
      setFeedback("⚠️ Unable to place bid. Please try again.");
    }
  };

  // ─── Determine product status ───────────────────────────────────────
  function getStatus(product) {
    if (product.sold) return "sold";
    if (product.frozen) return "frozen";
    if (product.endTime && new Date(product.endTime) < new Date()) return "frozen";
    return "active";
  }

  // ─── Apply filters (status, category, search) ──────────────────────
  const filteredListings = listings.filter((item) => {
    const statusMatch =
      sales.toLowerCase() === "all" ? true : getStatus(item) === sales.toLowerCase();
    const categoryMatch = activeCategory === "ALL" ? true : item.category === activeCategory;
    const searchMatch =
      !searchText ||
      item.productName.toLowerCase().includes(searchText.toLowerCase());
    return statusMatch && categoryMatch && searchMatch;
  });

  const resetFilters = () => {
    setActiveCategory(categories[0]?.value);
    setSales(filterOptions.sales[0].value);
  };

  return (
    <div className={styles.root}>
      {/* ─── 1. Top Bar ────────────────────────────────────────────────── */}
      <div className={styles.topBar}>
        <div className={styles.search}>
          <input
            type="text"
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            placeholder="Search Products"
            className={styles.searchInput}
          />
        </div>

        <div className={styles.topBarRight}>
          {/* Wallet Component */}
          <Wallet />

          {/* 👋 "Hi, FirstName" with styled background */}
          <div className={styles.greeting}>
            Hi, <span className={styles.username}>{firstName || "User"}</span>
          </div>
        </div>
      </div>

      {/* ─── 2. Main Carousel ───────────────────────────────────────────── */}
      <div className={styles.carouselWrapper}>
        <div className={styles.carouselScroll} ref={scrollRef}>
          {carouselItems.map((item) => (
            <div key={item.id} className={styles.carouselSlide}>
              <div className={styles.carouselContent}>
                <h2 className={styles.carouselTitle}>{item.title}</h2>
                <p className={styles.carouselText}>{item.text}</p>
                <button className={styles.carouselButton}>
                  {item.buttonText}
                </button>
              </div>
            </div>
          ))}
        </div>
        <div className={styles.carouselArrows}>
          <button
            onClick={prev}
            className={styles.arrowButton}
            disabled={current === 0}
          >
            ‹
          </button>
          <button
            onClick={next}
            className={styles.arrowButton}
            disabled={current === carouselItems.length - 1}
          >
            ›
          </button>
        </div>
        <div className={styles.carouselDots}>
          {carouselItems.map((_, i) => (
            <div
              key={i}
              onClick={() => setCurrent(i)}
              className={i === current ? styles.dotActive : styles.dot}
            />
          ))}
        </div>
      </div>

      {/* ─── 3. Top Categories Carousel ────────────────────────────────── */}
      <section className={styles.categorySection}>
        <h2 className={styles.categoryHeading}>Top Categories</h2>
        <div className={styles.categoryCarouselWrapper}>
          <button
            onClick={(e) => {
              e.preventDefault();
              prevCat();
            }}
            disabled={catIndex === 0}
            className={`${styles.carouselArrow} ${styles.carouselArrowLeft}`}
          >
            ‹
          </button>
          <div className={styles.categoryCarousel} ref={catRef}>
            {categories.map((cat) => (
              <div
                key={cat.value}
                className={`${styles.categoryCard} ${activeCategory === cat.value ? styles.active : ""
                  }`}
                onClick={() => setActiveCategory(cat.value)}
              >
                <div className={styles.categoryImageWrapper}>
                  <img
                    src={cat.image}
                    alt={cat.label}
                    className={styles.categoryImage}
                  />
                </div>
                <span className={styles.categoryLabel}>{cat.label}</span>
              </div>
            ))}
          </div>
          <button
            onClick={(e) => {
              e.preventDefault();
              nextCat();
            }}
            disabled={catIndex === categories.length - 1}
            className={`${styles.carouselArrow} ${styles.carouselArrowRight}`}
          >
            ›
          </button>
        </div>
      </section>

      {/* ─── 4. Listing Section ─────────────────────────────────────────── */}
      <div className={listStyles.listingWrapper}>
        {/* Sidebar Filters */}
        <aside className={listStyles.sidebar}>
          {/* Status Filter */}
          <div className={listStyles.filterGroup}>
            <label className={listStyles.filterTitle}>Status</label>
            <select
              value={sales}
              onChange={(e) => setSales(e.target.value)}
              className={listStyles.filterSelect}
            >
              {filterOptions.sales.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>

          {/* Category Filter */}
          <div className={listStyles.filterGroup}>
            <label className={listStyles.filterTitle}>Category</label>
            <select
              value={activeCategory}
              onChange={(e) => setActiveCategory(e.target.value)}
              className={listStyles.filterSelect}
            >
              {categories.map((cat) => (
                <option key={cat.value} value={cat.value}>
                  {cat.label}
                </option>
              ))}
            </select>
          </div>

          {/* Reset Button */}
          <button className={listStyles.applyButton} onClick={resetFilters}>
            Reset
          </button>
        </aside>

        {/* Results Grid */}
        <section className={listStyles.listingContent}>
          <div className={listStyles.listingHeader}>
            <div>
              Showing 1–{filteredListings.length} of {filteredListings.length} Results
            </div>
          </div>
          <div className={listStyles.grid}>
            {filteredListings.map((item) => (
              <div key={item.productId} className={listStyles.card}>
                <div
                  className={listStyles.cardImage}
                  style={{
                    backgroundImage: `url(${categories.find((c) => c.value === item.category)?.image ||
                      "/images/default.png"
                      })`,
                  }}
                />
                <div
                  className={
                    getStatus(item) === "active"
                      ? listStyles.badgeLive
                      : listStyles.badgeUpcoming
                  }
                >
                  {getStatus(item).toUpperCase()}
                </div>
                <div className={listStyles.countdown}>
                  {item.endTime ? (
                    <CountdownTimer endTime={item.endTime} />
                  ) : (
                    ["Days", "Hours", "Min", "Sec"].map((label) => (
                      <div key={label} className={styles.timeSegment}>
                        <span>00</span>
                        <small>{label}</small>
                      </div>
                    ))
                  )}
                </div>
                <h3 className={listStyles.cardTitle}>{item.productName}</h3>
                <div className={listStyles.currentBid}>
                  Current Bid at: <strong>${item.currentBid}</strong>
                </div>
                <button
                  className={listStyles.bidButton}
                  onClick={() => openBidModal(item)}
                  disabled={getStatus(item) !== "active"}
                >
                  {getStatus(item) === "active" ? "Bid Now" : "Notify Me"}
                </button>
              </div>
            ))}
          </div>
        </section>
      </div>

      {/* ─── 5. Bid Modal ─────────────────────────────────────────────────── */}
      {modalOpen && selectedProduct && (
        <div className={styles.modalOverlay} onClick={closeBidModal}>
          <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
            {/* Close Button */}
            <button className={styles.modalClose} onClick={closeBidModal} aria-label="Close">
              ✕
            </button>

            {/* Header Section with Gradient */}
            <div className={styles.modalHeader}>
              <div className={styles.modalHeaderIcon}>🎯</div>
              <h2 className={styles.modalTitle}>Place Your Bid</h2>
              <p className={styles.modalSubtitle}>Make an offer on this amazing item</p>
            </div>

            {/* Product Info Card */}
            <div className={styles.productInfoCard}>
              <div className={styles.productIcon}>📦</div>
              <div className={styles.productDetails}>
                <h3 className={styles.productName}>{selectedProduct.productName}</h3>
                <p className={styles.productDescription}>
                  {selectedProduct.description || "No description available."}
                </p>
              </div>
            </div>

            {/* Current Status Badge */}
            <div className={styles.currentBidBadge}>
              <div className={styles.badgeIcon}>💰</div>
              <div className={styles.badgeContent}>
                <span className={styles.badgeLabel}>Current Highest Bid</span>
                <span className={styles.badgeValue}>${selectedProduct.currentBid?.toFixed(2) || "0.00"}</span>
              </div>
            </div>

            {/* Bid Statistics Grid */}
            <div className={styles.bidStatsContainer}>
              <h3 className={styles.statsTitle}>📊 Bidding Statistics</h3>
              <div className={styles.bidInfoGrid}>
                {bidStatus.maxBidAmount > 0 && (
                  <div className={styles.bidInfoItem}>
                    <div className={styles.bidInfoIcon}>🏆</div>
                    <div className={styles.bidInfoContent}>
                      <span className={styles.bidInfoLabel}>Highest Bid</span>
                      <span className={styles.bidInfoValue}>${bidStatus.maxBidAmount.toFixed(2)}</span>
                    </div>
                  </div>
                )}
                
                {bidStatus.totalBidders > 0 && (
                  <div className={styles.bidInfoItem}>
                    <div className={styles.bidInfoIcon}>👥</div>
                    <div className={styles.bidInfoContent}>
                      <span className={styles.bidInfoLabel}>Active Bidders</span>
                      <span className={styles.bidInfoValue}>{bidStatus.totalBidders}</span>
                    </div>
                  </div>
                )}
                
                {bidStatus.averageBidAmount > 0 && (
                  <div className={styles.bidInfoItem}>
                    <div className={styles.bidInfoIcon}>📈</div>
                    <div className={styles.bidInfoContent}>
                      <span className={styles.bidInfoLabel}>Average Bid</span>
                      <span className={styles.bidInfoValue}>${bidStatus.averageBidAmount.toFixed(2)}</span>
                    </div>
                  </div>
                )}
                
                <div className={styles.bidInfoItem}>
                  <div className={styles.bidInfoIcon}>⬇️</div>
                  <div className={styles.bidInfoContent}>
                    <span className={styles.bidInfoLabel}>Minimum Bid</span>
                    <span className={styles.bidInfoValue}>${selectedProduct.minBid?.toFixed(2) || "0.00"}</span>
                  </div>
                </div>
                
                <div className={styles.bidInfoItem}>
                  <div className={styles.bidInfoIcon}>⬆️</div>
                  <div className={styles.bidInfoContent}>
                    <span className={styles.bidInfoLabel}>Maximum Bid</span>
                    <span className={styles.bidInfoValue}>${selectedProduct.maxBid?.toFixed(2) || "0.00"}</span>
                  </div>
                </div>
              </div>
            </div>

            {/* User Bid Warning */}
            {bidStatus.hasUserBid && (
              <div className={styles.userBidNotice}>
                <div className={styles.noticeIcon}>⚠️</div>
                <div className={styles.noticeContent}>
                  <strong>You have already placed a bid</strong>
                  <p>Your current bid: <span className={styles.userBidAmount}>${bidStatus.userBidAmount.toFixed(2)}</span></p>
                  <small>You can only place one bid per product.</small>
                </div>
              </div>
            )}

            {/* Bid Input Section */}
            <div className={styles.bidInputSection}>
              <div className={styles.formGroup}>
                <label htmlFor="bidAmount" className={styles.inputLabel}>
                  <span className={styles.labelIcon}>💵</span>
                  Your Bid Amount
                </label>
                <div className={styles.inputWrapper}>
                  <span className={styles.currencySymbol}>$</span>
                  <input
                    id="bidAmount"
                    type="number"
                    value={bidAmount}
                    onChange={(e) => setBidAmount(e.target.value)}
                    placeholder={`${selectedProduct.minBid?.toFixed(2) || "0.00"}`}
                    min={selectedProduct.minBid}
                    max={selectedProduct.maxBid}
                    step="0.01"
                    disabled={bidStatus.hasUserBid}
                    className={styles.bidInput}
                  />
                </div>
                <div className={styles.bidHintBox}>
                  <span className={styles.hintIcon}>ℹ️</span>
                  <small className={styles.bidHint}>
                    Bid range: ${selectedProduct.minBid?.toFixed(2) || "0.00"} - ${selectedProduct.maxBid?.toFixed(2) || "0.00"}
                    {selectedProduct.currentBid > 0 && (
                      <span className={styles.currentBidHint}>
                        {" "}• Must exceed ${selectedProduct.currentBid.toFixed(2)}
                      </span>
                    )}
                  </small>
                </div>
              </div>
            </div>

            {/* Feedback Message */}
            {feedback && (
              <div
                className={
                  feedback.startsWith("✅")
                    ? styles.successMsg
                    : styles.errorMsg
                }
              >
                <span className={styles.feedbackIcon}>
                  {feedback.startsWith("✅") ? "✅" : "❌"}
                </span>
                <span>{feedback.replace(/^[✅❌⚠️]\s*/, "")}</span>
              </div>
            )}

            {/* Action Buttons */}
            <div className={styles.modalActions}>
              <button 
                onClick={handleProceedToPayment} 
                className={styles.bidNowButton}
                disabled={bidStatus.hasUserBid}
              >
                <span className={styles.buttonIcon}>🎯</span>
                <span>{bidStatus.hasUserBid ? "Already Bid" : "Place Bid"}</span>
              </button>
              <button onClick={closeBidModal} className={styles.cancelButton}>
                <span className={styles.buttonIcon}>✕</span>
                <span>Cancel</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
