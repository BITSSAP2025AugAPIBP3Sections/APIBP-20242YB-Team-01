import React, { useState, useRef, useEffect } from "react";
import filterOptions from "../data/filters.json";
import styles from "../css/ExplorePage.module.css";
import listStyles from "../css/ListingSection.module.css";
import { useSection } from "../context/SectionContext";

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

    // decode user ID from token
    // const user = jwtDecode(token);
    const userId = localStorage.getItem("id");

    // total bidders
    const totalBidders = bids.length;

    // max bid
    const maxBidAmount =
      bids.length > 0
        ? Math.max(...bids.map((b) => b.amount))
        : 0;

    // average bid
    const averageBidAmount =
      bids.length > 0
        ? bids.reduce((sum, b) => sum + b.amount, 0) / bids.length
        : 0;

    // check if user has bid
    const userBid = bids.find((b) => b.bidderId === userId);

    const hasUserBid = !!userBid;
    const userBidAmount = userBid ? userBid.amount : 0;

    // set state
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

    const amt = parseFloat(bidAmount);
    if (isNaN(amt) || amt <= selectedProduct.currentBid) {
      setFeedback("⚠️ The Bid amount must be greater than the current bid.");
      return;
    }
    if (amt < selectedProduct.minBid || amt > selectedProduct.maxBid) {
      setFeedback(
          `⚠️ Bid must be between $${selectedProduct.minBid} and $${selectedProduct.maxBid}.`
      );
      return;
    }

    const token = localStorage.getItem("token");
    try {
  const token = localStorage.getItem("token");

  // const user = jwtDecode(token);
    const userId = localStorage.getItem("id");

  const bidsRes = await fetch(
    `http://localhost:8080/api/bids/v1/products/${selectedProduct.productId}`,
    {
      headers: { Authorization: `Bearer ${token}` },
    }
  );

  if (!bidsRes.ok) {
    throw new Error("Failed to fetch bids");
  }

  const bids = await bidsRes.json();

  // check if this user has already placed a bid
  const alreadyBid = bids.some(bid => bid.bidderId === userId);

  if (alreadyBid) {
    alert("⚠️ You have already placed a bid on this product.");
    closeBidModal();
    return;
  }

  // continue to payment page
  localStorage.setItem(
    "pendingBid",
    JSON.stringify({
      productId: selectedProduct.productId,
      bidAmount: amt,
    })
  );
  setModalOpen(false);
  setSection("payment");

} catch (err) {
  console.error("Error checking bid:", err);
  setFeedback("⚠️ Unable to verify bid status. Please try again.");
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

          {/* 👋 “Hi, FirstName” with styled background */}
          <div className={styles.greeting}>
            Hi, <span className={styles.username}>{firstName || "User"}</span>
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
                  <div className={styles.imagesStack}>
                    <div className={styles.imagesStackItem} />
                    <div className={styles.imagesStackItem} />
                    <div className={styles.imagesStackItem} />
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
                      className={`${styles.categoryCard} ${
                          activeCategory === cat.value ? styles.active : ""
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
                          backgroundImage: `url(${
                              categories.find((c) => c.value === item.category)?.image ||
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
                <h2>Place a Bid</h2>
                <p>
                  <strong>Item:</strong> {selectedProduct.productName}
                </p>
                <p className={styles.modalDescription}>
                  <strong>Description:</strong>{" "}
                  {selectedProduct.description || "No description available."}
                </p>

                <p>
                  <strong>Current Bid:</strong> ${selectedProduct.currentBid}
                </p>
                <p>
                  <strong>Average Bid:</strong> ${bidStatus.averageBidAmount.toFixed(2)}
                </p>
                <p>
                  <strong>Number of Bidders:</strong> {bidStatus.totalBidders}
                </p>
                <p>
                  <strong>Allowed Range:</strong> ${selectedProduct.minBid} – $
                  {selectedProduct.maxBid}
                </p>

                <div className={styles.formGroup}>
                  <label htmlFor="bidAmount">Your Bid</label>
                  <input
                      id="bidAmount"
                      type="number"
                      value={bidAmount}
                      onChange={(e) => setBidAmount(e.target.value)}
                      placeholder="Enter higher than current bid"
                  />
                </div>

                {feedback && (
                    <div
                        className={
                          feedback.startsWith("✅")
                              ? styles.successMsg
                              : styles.errorMsg
                        }
                    >
                      {feedback}
                    </div>
                )}

                <div className={styles.modalActions}>
                  <button onClick={handleProceedToPayment} className="btn">
                    Proceed to Payment
                  </button>
                </div>
              </div>
            </div>
        )}
      </div>
  );
}
