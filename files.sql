-- phpMyAdmin SQL Dump
-- version 5.2.3
-- https://www.phpmyadmin.net/
--
-- Host: localhost:3306
-- Generation Time: Aug 20, 2026 at 05:11 PM
-- Server version: 10.11.18-MariaDB-cll-lve
-- PHP Version: 8.4.24

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `sxlvhdzo_driving_school`
--

-- --------------------------------------------------------

--
-- Table structure for table `files`
--

CREATE TABLE `files` (
  `id` int(11) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` text NOT NULL,
  `file_path` varchar(500) NOT NULL,
  `file_type` varchar(100) NOT NULL,
  `file_size` int(11) NOT NULL,
  `folder_id` int(11) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT current_timestamp(3),
  `updated_at` datetime(3) NOT NULL,
  `thumbnail_url` varchar(500) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `files`
--

INSERT INTO `files` (`id`, `name`, `description`, `file_path`, `file_type`, `file_size`, `folder_id`, `created_at`, `updated_at`, `thumbnail_url`) VALUES
(8, 'Igazete_Kinyarwanda', '', '/uploads/files/1768396910063-96gyk7a3v1u.pdf', 'application/pdf', 5924655, NULL, '2026-01-14 13:21:50.882', '2026-01-14 13:21:50.882', '/uploads/learning-materials/thumbnails/thumb-1768396910063.svg'),
(9, 'New File', '', '/uploads/files/1768398742126-2zgpcsrptcl.jpg', 'image/jpeg', 21262, NULL, '2026-01-14 13:52:22.339', '2026-01-14 13:52:22.339', '/uploads/learning-materials/thumbnails/thumb-1768398742126.svg'),
(10, 'IMG-20251218-WA0128', '', '/uploads/files/1768398753634-232e90brrsmi.jpg', 'image/jpeg', 90005, NULL, '2026-01-14 13:52:33.637', '2026-01-14 13:52:33.637', '/uploads/learning-materials/thumbnails/thumb-1768398753634.svg'),
(11, 'IMG-20251218-WA0129', '', '/uploads/files/1768398761625-gdnnt0gu98g.jpg', 'image/jpeg', 28910, NULL, '2026-01-14 13:52:41.628', '2026-01-14 13:52:41.628', '/uploads/learning-materials/thumbnails/thumb-1768398761625.svg'),
(12, 'IMG-20251218-WA0130', '', '/uploads/files/1768398770496-lowi8rwocva.jpg', 'image/jpeg', 23338, NULL, '2026-01-14 13:52:50.499', '2026-01-14 13:52:50.499', '/uploads/learning-materials/thumbnails/thumb-1768398770496.svg'),
(13, 'IMG-20251218-WA0131', '', '/uploads/files/1768398779758-h5tvm5ic9ab.jpg', 'image/jpeg', 54835, NULL, '2026-01-14 13:52:59.760', '2026-01-14 13:52:59.760', '/uploads/learning-materials/thumbnails/thumb-1768398779758.svg'),
(14, 'IMG-20251218-WA0132', '', '/uploads/files/1768398794036-nbjmgcud8qg.jpg', 'image/jpeg', 104361, NULL, '2026-01-14 13:53:14.116', '2026-01-14 13:53:14.116', '/uploads/learning-materials/thumbnails/thumb-1768398794036.svg'),
(15, 'IMG-20251218-WA0133', '', '/uploads/files/1768398803185-b3wq8x8w8ei.jpg', 'image/jpeg', 52097, NULL, '2026-01-14 13:53:23.188', '2026-01-14 13:53:23.188', '/uploads/learning-materials/thumbnails/thumb-1768398803185.svg'),
(16, 'IMG-20251218-WA0134', '', '/uploads/files/1768398811235-w3dhgegeqhm.jpg', 'image/jpeg', 18922, NULL, '2026-01-14 13:53:31.238', '2026-01-14 13:53:31.238', '/uploads/learning-materials/thumbnails/thumb-1768398811235.svg'),
(17, 'IMG-20251218-WA0136', '', '/uploads/files/1768398822499-igob7gl3j9.jpg', 'image/jpeg', 64716, NULL, '2026-01-14 13:53:42.502', '2026-01-14 13:53:42.502', '/uploads/learning-materials/thumbnails/thumb-1768398822499.svg'),
(18, 'IMG-20251218-WA0137', '', '/uploads/files/1768398834003-ukzchgfu8is.jpg', 'image/jpeg', 31695, NULL, '2026-01-14 13:53:54.010', '2026-01-14 13:53:54.010', '/uploads/learning-materials/thumbnails/thumb-1768398834003.svg'),
(19, 'IMG-20251218-WA0138', '', '/uploads/files/1768398847538-1xw3q3pgl54.jpg', 'image/jpeg', 104798, NULL, '2026-01-14 13:54:07.746', '2026-01-14 13:54:07.746', '/uploads/learning-materials/thumbnails/thumb-1768398847538.svg'),
(20, 'IMG-20251218-WA0139', '', '/uploads/files/1768398859660-yate7d0s2g.jpg', 'image/jpeg', 98793, NULL, '2026-01-14 13:54:19.662', '2026-01-14 13:54:19.662', '/uploads/learning-materials/thumbnails/thumb-1768398859660.svg'),
(21, 'IMG-20251218-WA0140', '', '/uploads/files/1768398868189-5f1z34217k4.jpg', 'image/jpeg', 23916, NULL, '2026-01-14 13:54:28.191', '2026-01-14 13:54:28.191', '/uploads/learning-materials/thumbnails/thumb-1768398868189.svg'),
(22, 'IMG-20251218-WA0141', '', '/uploads/files/1768398875801-6dyg4m8j1y9.jpg', 'image/jpeg', 27331, NULL, '2026-01-14 13:54:35.802', '2026-01-14 13:54:35.802', '/uploads/learning-materials/thumbnails/thumb-1768398875801.svg'),
(23, 'IMG-20251218-WA0142', '', '/uploads/files/1768398883492-5tshonwupd9.jpg', 'image/jpeg', 35026, NULL, '2026-01-14 13:54:43.496', '2026-01-14 13:54:43.496', '/uploads/learning-materials/thumbnails/thumb-1768398883492.svg'),
(24, 'IMG-20251218-WA0143', '', '/uploads/files/1768398891478-61cexotz9si.jpg', 'image/jpeg', 30149, NULL, '2026-01-14 13:54:51.481', '2026-01-14 13:54:51.481', '/uploads/learning-materials/thumbnails/thumb-1768398891478.svg'),
(25, 'IMG-20251218-WA0144', '', '/uploads/files/1768398902649-k643w3odae.jpg', 'image/jpeg', 135635, NULL, '2026-01-14 13:55:02.653', '2026-01-14 13:55:02.653', '/uploads/learning-materials/thumbnails/thumb-1768398902649.svg'),
(26, 'New File', '', '/uploads/files/1768412985297-u3czya642sn.png', 'image/png', 20274, NULL, '2026-01-14 17:49:45.351', '2026-01-14 17:49:45.351', '/uploads/learning-materials/thumbnails/thumb-1768412985297.svg'),
(27, 'icyapa2', '', '/uploads/files/1768413361369-95pfjp47y2.png', 'image/png', 2896, NULL, '2026-01-14 17:56:01.460', '2026-01-14 17:56:01.460', '/uploads/learning-materials/thumbnails/thumb-1768413361369.svg'),
(28, 'Screenshot 2026-01-14 195445', '', '/uploads/files/1768413379383-6r80gmlsa6.png', 'image/png', 19091, NULL, '2026-01-14 17:56:19.453', '2026-01-14 17:56:19.453', '/uploads/learning-materials/thumbnails/thumb-1768413379383.svg'),
(29, 'Screenshot 2026-01-14 195459', '', '/uploads/files/1768413389043-8gbfmdrojne.png', 'image/png', 6332, NULL, '2026-01-14 17:56:29.045', '2026-01-14 17:56:29.045', '/uploads/learning-materials/thumbnails/thumb-1768413389043.svg'),
(30, 'Screenshot 2026-01-14 195532', '', '/uploads/files/1768413406182-k66u70wtifs.png', 'image/png', 20882, NULL, '2026-01-14 17:56:46.184', '2026-01-14 17:56:46.184', '/uploads/learning-materials/thumbnails/thumb-1768413406182.svg'),
(31, 'Screenshot 2026-01-14 195737', '', '/uploads/files/1768413471500-qid2vqdyf1p.png', 'image/png', 8869, NULL, '2026-01-14 17:57:53.732', '2026-01-14 17:57:53.732', '/uploads/learning-materials/thumbnails/thumb-1768413471500.svg'),
(32, 'Screenshot 2026-01-14 195837', '', '/uploads/files/1768413529664-cy9aeyxfyno.png', 'image/png', 5932, NULL, '2026-01-14 17:58:52.114', '2026-01-14 17:58:52.114', '/uploads/learning-materials/thumbnails/thumb-1768413529664.svg'),
(33, 'Screenshot 2026-01-14 195927', '', '/uploads/files/1768413582143-fn3qpmqpvm5.png', 'image/png', 1837, NULL, '2026-01-14 17:59:42.181', '2026-01-14 17:59:42.181', '/uploads/learning-materials/thumbnails/thumb-1768413582143.svg'),
(34, 'Screenshot 2026-01-14 200000', '', '/uploads/files/1768413616795-hcm1o9ydmft.png', 'image/png', 10111, NULL, '2026-01-14 18:00:16.948', '2026-01-14 18:00:16.948', '/uploads/learning-materials/thumbnails/thumb-1768413616795.svg'),
(35, 'Screenshot 2026-01-14 200028', '', '/uploads/files/1768413638997-bs5mxy37wqa.png', 'image/png', 802, NULL, '2026-01-14 18:00:39.000', '2026-01-14 18:00:39.000', '/uploads/learning-materials/thumbnails/thumb-1768413638997.svg'),
(36, 'Screenshot 2026-01-14 200057', '', '/uploads/files/1768413673264-it2ufz9sp9d.png', 'image/png', 1584, NULL, '2026-01-14 18:01:15.683', '2026-01-14 18:01:15.683', '/uploads/learning-materials/thumbnails/thumb-1768413673264.svg'),
(37, 'Screenshot 2026-01-14 200118', '', '/uploads/files/1768413691304-y9vxh5ho0nm.png', 'image/png', 1910, NULL, '2026-01-14 18:01:31.306', '2026-01-14 18:01:31.306', '/uploads/learning-materials/thumbnails/thumb-1768413691304.svg'),
(38, 'Screenshot 2026-01-14 200146', '', '/uploads/files/1768413715068-iwfveo9vq4.png', 'image/png', 633, NULL, '2026-01-14 18:01:55.070', '2026-01-14 18:01:55.070', '/uploads/learning-materials/thumbnails/thumb-1768413715068.svg'),
(39, 'Screenshot 2026-01-14 195532', '', '/uploads/files/1768413780897-7kum3e0r2mj.png', 'image/png', 20882, NULL, '2026-01-14 18:03:00.976', '2026-01-14 18:03:00.976', '/uploads/learning-materials/thumbnails/thumb-1768413780897.svg'),
(40, 'Screenshot 2026-01-14 200310', '', '/uploads/files/1768413807949-dxma8dqpy4t.png', 'image/png', 11400, NULL, '2026-01-14 18:03:28.007', '2026-01-14 18:03:28.007', '/uploads/learning-materials/thumbnails/thumb-1768413807949.svg'),
(41, 'Screenshot 2026-01-14 200341', '', '/uploads/files/1768413829206-shr0qpvs32.png', 'image/png', 6299, NULL, '2026-01-14 18:03:49.226', '2026-01-14 18:03:49.226', '/uploads/learning-materials/thumbnails/thumb-1768413829206.svg'),
(42, 'Screenshot 2026-01-14 200359', '', '/uploads/files/1768413851583-1l4hzk8znsc.png', 'image/png', 11369, NULL, '2026-01-14 18:04:11.678', '2026-01-14 18:04:11.678', '/uploads/learning-materials/thumbnails/thumb-1768413851583.svg'),
(43, 'Screenshot 2026-01-14 200421', '', '/uploads/files/1768413870952-8wqizzckprk.png', 'image/png', 14379, NULL, '2026-01-14 18:04:30.954', '2026-01-14 18:04:30.954', '/uploads/learning-materials/thumbnails/thumb-1768413870952.svg'),
(44, 'Screenshot 2026-01-14 200447', '', '/uploads/files/1768413898151-gkt4mh64za6.png', 'image/png', 18777, NULL, '2026-01-14 18:04:58.158', '2026-01-14 18:04:58.158', '/uploads/learning-materials/thumbnails/thumb-1768413898151.svg'),
(45, 'Screenshot 2026-01-14 200518', '', '/uploads/files/1768413927848-zbf45mh2as.png', 'image/png', 13267, NULL, '2026-01-14 18:05:30.265', '2026-01-14 18:05:30.265', '/uploads/learning-materials/thumbnails/thumb-1768413927848.svg'),
(46, 'Screenshot 2026-01-14 200540', '', '/uploads/files/1768413955911-8ifsydg5g9j.png', 'image/png', 10551, NULL, '2026-01-14 18:05:55.914', '2026-01-14 18:05:55.914', '/uploads/learning-materials/thumbnails/thumb-1768413955911.svg'),
(47, 'Screenshot 2026-01-14 200609', '', '/uploads/files/1768413981563-n73p75xlask.png', 'image/png', 3882, NULL, '2026-01-14 18:06:21.686', '2026-01-14 18:06:21.686', '/uploads/learning-materials/thumbnails/thumb-1768413981563.svg'),
(48, 'Screenshot 2026-01-14 200628', '', '/uploads/files/1768413999329-sem5v732vns.png', 'image/png', 14673, NULL, '2026-01-14 18:06:39.331', '2026-01-14 18:06:39.331', '/uploads/learning-materials/thumbnails/thumb-1768413999329.svg'),
(49, 'Screenshot 2026-01-14 200710', '', '/uploads/files/1768414039300-35232uwuzz3.png', 'image/png', 15262, NULL, '2026-01-14 18:07:19.305', '2026-01-14 18:07:19.305', '/uploads/learning-materials/thumbnails/thumb-1768414039300.svg'),
(50, 'Screenshot 2026-01-14 200740', '', '/uploads/files/1768414069447-5ykw5iza6s.png', 'image/png', 15391, NULL, '2026-01-14 18:07:49.451', '2026-01-14 18:07:49.451', '/uploads/learning-materials/thumbnails/thumb-1768414069447.svg'),
(51, 'Screenshot 2026-01-14 200814', '', '/uploads/files/1768414106636-kcopz5vx6e8.png', 'image/png', 7460, NULL, '2026-01-14 18:08:26.773', '2026-01-14 18:08:26.773', '/uploads/learning-materials/thumbnails/thumb-1768414106636.svg'),
(52, 'Screenshot 2026-01-14 200909', '', '/uploads/files/1768414168177-73rb5nqji92.png', 'image/png', 7512, NULL, '2026-01-14 18:09:28.247', '2026-01-14 18:09:28.247', '/uploads/learning-materials/thumbnails/thumb-1768414168177.svg'),
(53, 'Screenshot 2026-01-14 200947', '', '/uploads/files/1768414212829-oh9gwsessv.png', 'image/png', 1592, NULL, '2026-01-14 18:10:13.249', '2026-01-14 18:10:13.249', '/uploads/learning-materials/thumbnails/thumb-1768414212829.svg'),
(54, 'Screenshot 2026-01-14 201021', '', '/uploads/files/1768414230765-c48vtn9vn1t.png', 'image/png', 5129, NULL, '2026-01-14 18:10:30.767', '2026-01-14 18:10:30.767', '/uploads/learning-materials/thumbnails/thumb-1768414230765.svg'),
(55, 'Screenshot 2026-01-14 201049', '', '/uploads/files/1768414260280-ojvo25easri.png', 'image/png', 4349, NULL, '2026-01-14 18:11:00.282', '2026-01-14 18:11:00.282', '/uploads/learning-materials/thumbnails/thumb-1768414260280.svg'),
(56, 'Screenshot 2026-01-14 201111', '', '/uploads/files/1768414285037-54z2gq6p9cc.png', 'image/png', 1730, NULL, '2026-01-14 18:11:25.134', '2026-01-14 18:11:25.134', '/uploads/learning-materials/thumbnails/thumb-1768414285037.svg'),
(57, 'Screenshot 2026-01-14 201209', '', '/uploads/files/1768414345713-t4c556l2pbf.png', 'image/png', 16249, NULL, '2026-01-14 18:12:25.828', '2026-01-14 18:12:25.828', '/uploads/learning-materials/thumbnails/thumb-1768414345713.svg'),
(58, 'Screenshot 2026-01-14 201236', '', '/uploads/files/1768414369920-7ql8xpx01jm.png', 'image/png', 15904, NULL, '2026-01-14 18:12:49.923', '2026-01-14 18:12:49.923', '/uploads/learning-materials/thumbnails/thumb-1768414369920.svg'),
(59, 'Screenshot 2026-01-14 201725', '', '/uploads/files/1768414659286-u0j87k2xwpi.png', 'image/png', 9841, NULL, '2026-01-14 18:17:39.381', '2026-01-14 18:17:39.381', '/uploads/learning-materials/thumbnails/thumb-1768414659286.svg'),
(60, 'Screenshot 2026-01-14 201756', '', '/uploads/files/1768414692105-zfsvtmwl3u9.png', 'image/png', 2036, NULL, '2026-01-14 18:18:12.257', '2026-01-14 18:18:12.257', '/uploads/learning-materials/thumbnails/thumb-1768414692105.svg'),
(61, 'Screenshot 2026-01-14 201833', '', '/uploads/files/1768414730251-ocl6vxnakk.png', 'image/png', 7470, NULL, '2026-01-14 18:18:50.258', '2026-01-14 18:18:50.258', '/uploads/learning-materials/thumbnails/thumb-1768414730251.svg'),
(62, 'Screenshot 2026-01-14 201924', '', '/uploads/files/1768414770975-s2o49q6y84b.png', 'image/png', 2225, NULL, '2026-01-14 18:19:30.979', '2026-01-14 18:19:30.979', '/uploads/learning-materials/thumbnails/thumb-1768414770975.svg'),
(63, 'Screenshot 2026-01-14 201947', '', '/uploads/files/1768414799092-9a8j15ty3tf.png', 'image/png', 1602, NULL, '2026-01-14 18:19:59.094', '2026-01-14 18:19:59.094', '/uploads/learning-materials/thumbnails/thumb-1768414799092.svg'),
(64, 'Screenshot 2026-01-14 202020', '', '/uploads/files/1768414987511-cy24kd6zshb.png', 'image/png', 15282, NULL, '2026-01-14 18:23:07.586', '2026-01-14 18:23:07.586', '/uploads/learning-materials/thumbnails/thumb-1768414987511.svg'),
(65, 'Screenshot 2026-01-14 202256', '', '/uploads/files/1768415001130-w5ztyoxzbh.png', 'image/png', 19895, NULL, '2026-01-14 18:23:21.135', '2026-01-14 18:23:21.135', '/uploads/learning-materials/thumbnails/thumb-1768415001130.svg'),
(66, 'Screenshot 2026-01-14 202412', '', '/uploads/files/1768415070144-xzfc3hlsq6d.png', 'image/png', 13579, NULL, '2026-01-14 18:24:30.210', '2026-01-14 18:24:30.210', '/uploads/learning-materials/thumbnails/thumb-1768415070144.svg'),
(67, 'Screenshot 2026-01-14 202436', '', '/uploads/files/1768415088376-5t6artpiufp.png', 'image/png', 3384, NULL, '2026-01-14 18:24:48.378', '2026-01-14 18:24:48.378', '/uploads/learning-materials/thumbnails/thumb-1768415088376.svg'),
(68, 'Screenshot 2026-01-14 202512', '', '/uploads/files/1768415125462-o4hkzo4g3y.png', 'image/png', 37766, NULL, '2026-01-14 18:25:25.584', '2026-01-14 18:25:25.584', '/uploads/learning-materials/thumbnails/thumb-1768415125462.svg'),
(69, 'Screenshot 2026-01-14 202545', '', '/uploads/files/1768415154233-vob7d945rs.png', 'image/png', 4960, NULL, '2026-01-14 18:25:54.236', '2026-01-14 18:25:54.236', '/uploads/learning-materials/thumbnails/thumb-1768415154233.svg'),
(70, 'Screenshot 2026-01-14 202606', '', '/uploads/files/1768415180754-u7aahik8up.png', 'image/png', 6639, NULL, '2026-01-14 18:26:20.963', '2026-01-14 18:26:20.963', '/uploads/learning-materials/thumbnails/thumb-1768415180754.svg'),
(71, 'Screenshot 2026-01-14 202630', '', '/uploads/files/1768415200944-rulb96l4f48.png', 'image/png', 7634, NULL, '2026-01-14 18:26:40.949', '2026-01-14 18:26:40.949', '/uploads/learning-materials/thumbnails/thumb-1768415200944.svg'),
(72, 'Screenshot 2026-01-14 202655', '', '/uploads/files/1768415232272-r47v74og68.png', 'image/png', 1327, NULL, '2026-01-14 18:27:12.279', '2026-01-14 18:27:12.279', '/uploads/learning-materials/thumbnails/thumb-1768415232272.svg'),
(73, 'Screenshot 2026-01-14 202729', '', '/uploads/files/1768415257162-u06mbjtzdf.png', 'image/png', 14330, NULL, '2026-01-14 18:27:37.164', '2026-01-14 18:27:37.164', '/uploads/learning-materials/thumbnails/thumb-1768415257162.svg'),
(74, 'Screenshot 2026-01-14 202844', '', '/uploads/files/1768415333013-f685hplfet.png', 'image/png', 48570, NULL, '2026-01-14 18:28:53.050', '2026-01-14 18:28:53.050', '/uploads/learning-materials/thumbnails/thumb-1768415333013.svg'),
(75, 'New File', '', '/uploads/files/1768478593281-39e0i0twh8b.png', 'image/png', 19223, NULL, '2026-01-15 12:03:13.328', '2026-01-15 12:03:13.328', '/uploads/learning-materials/thumbnails/thumb-1768478593281.svg'),
(76, 'Screenshot 2026-01-15 140322', '', '/uploads/files/1768478610313-96hfsp6z1u.png', 'image/png', 12555, NULL, '2026-01-15 12:03:30.315', '2026-01-15 12:03:30.315', '/uploads/learning-materials/thumbnails/thumb-1768478610313.svg'),
(77, 'Screenshot 2026-01-15 140355', '', '/uploads/files/1768478647988-isuyl6yx3f.png', 'image/png', 10718, NULL, '2026-01-15 12:04:08.060', '2026-01-15 12:04:08.060', '/uploads/learning-materials/thumbnails/thumb-1768478647988.svg'),
(78, 'Screenshot 2026-01-15 140419', '', '/uploads/files/1768478668432-jrd7lo9k8in.png', 'image/png', 3956, NULL, '2026-01-15 12:04:28.434', '2026-01-15 12:04:28.434', '/uploads/learning-materials/thumbnails/thumb-1768478668432.svg'),
(79, 'Screenshot 2026-01-15 140438', '', '/uploads/files/1768478685520-4nggeozgtso.png', 'image/png', 14668, NULL, '2026-01-15 12:04:45.522', '2026-01-15 12:04:45.522', '/uploads/learning-materials/thumbnails/thumb-1768478685520.svg'),
(80, 'Screenshot 2026-01-15 140500', '', '/uploads/files/1768478712132-9m2nze8acb4.png', 'image/png', 15252, NULL, '2026-01-15 12:05:12.474', '2026-01-15 12:05:12.474', '/uploads/learning-materials/thumbnails/thumb-1768478712132.svg'),
(81, 'Screenshot 2026-01-15 140522', '', '/uploads/files/1768478730040-afb1z6dnbu.png', 'image/png', 15349, NULL, '2026-01-15 12:05:30.043', '2026-01-15 12:05:30.043', '/uploads/learning-materials/thumbnails/thumb-1768478730040.svg'),
(82, 'Screenshot 2026-01-15 140541', '', '/uploads/files/1768478747910-66azz9odzo9.png', 'image/png', 7744, NULL, '2026-01-15 12:05:47.914', '2026-01-15 12:05:47.914', '/uploads/learning-materials/thumbnails/thumb-1768478747910.svg'),
(83, 'Screenshot 2026-01-15 140601', '', '/uploads/files/1768478771307-xns5w7hjit.png', 'image/png', 1652, NULL, '2026-01-15 12:06:11.344', '2026-01-15 12:06:11.344', '/uploads/learning-materials/thumbnails/thumb-1768478771307.svg'),
(84, 'Screenshot 2026-01-15 140616', '', '/uploads/files/1768478783087-hjfymi7f3e4.png', 'image/png', 5859, NULL, '2026-01-15 12:06:23.090', '2026-01-15 12:06:23.090', '/uploads/learning-materials/thumbnails/thumb-1768478783087.svg'),
(85, 'Screenshot 2026-01-15 140633', '', '/uploads/files/1768478804422-huxcrouwsft.png', 'image/png', 4652, NULL, '2026-01-15 12:06:44.425', '2026-01-15 12:06:44.425', '/uploads/learning-materials/thumbnails/thumb-1768478804422.svg'),
(86, 'Screenshot 2026-01-15 140653', '', '/uploads/files/1768478819511-t7gcp91tmem.png', 'image/png', 1722, NULL, '2026-01-15 12:06:59.513', '2026-01-15 12:06:59.513', '/uploads/learning-materials/thumbnails/thumb-1768478819511.svg'),
(87, 'Screenshot 2026-01-15 140712', '', '/uploads/files/1768478843162-wn1bszbnz5o.png', 'image/png', 16182, NULL, '2026-01-15 12:07:23.235', '2026-01-15 12:07:23.235', '/uploads/learning-materials/thumbnails/thumb-1768478843162.svg'),
(88, 'Screenshot 2026-01-15 140729', '', '/uploads/files/1768478855808-ut7l66pgh4r.png', 'image/png', 16012, NULL, '2026-01-15 12:07:35.811', '2026-01-15 12:07:35.811', '/uploads/learning-materials/thumbnails/thumb-1768478855808.svg'),
(89, 'Screenshot 2026-01-15 140744', '', '/uploads/files/1768478871030-lyv05sxtef.png', 'image/png', 9777, NULL, '2026-01-15 12:07:51.032', '2026-01-15 12:07:51.032', '/uploads/learning-materials/thumbnails/thumb-1768478871030.svg'),
(90, 'Screenshot 2026-01-15 140804', '', '/uploads/files/1768478894323-epuatyerzpl.png', 'image/png', 2266, NULL, '2026-01-15 12:08:14.377', '2026-01-15 12:08:14.377', '/uploads/learning-materials/thumbnails/thumb-1768478894323.svg'),
(91, 'New File', '', '/uploads/files/1768479219346-pt1n4sl97vr.png', 'image/png', 4942, NULL, '2026-01-15 12:13:39.403', '2026-01-15 12:13:39.403', '/uploads/learning-materials/thumbnails/thumb-1768479219346.svg'),
(92, 'New File', '', '/uploads/files/1768501724107-no14tjqzm3.png', 'image/png', 4775, NULL, '2026-01-15 18:28:44.112', '2026-01-15 18:28:44.112', '/uploads/learning-materials/thumbnails/thumb-1768501724107.svg'),
(93, 'Screenshot 2026-01-15 202903', '', '/uploads/files/1768501756263-7kv05a138w9.png', 'image/png', 13690, NULL, '2026-01-15 18:29:16.334', '2026-01-15 18:29:16.334', '/uploads/learning-materials/thumbnails/thumb-1768501756263.svg'),
(94, 'Screenshot 2026-01-15 203000', '', '/uploads/files/1768501815710-8dc1tuc7pd8.png', 'image/png', 22476, NULL, '2026-01-15 18:30:15.785', '2026-01-15 18:30:15.785', '/uploads/learning-materials/thumbnails/thumb-1768501815710.svg'),
(95, 'Screenshot 2026-01-15 203044', '', '/uploads/files/1768501863768-etvelzdp6mf.png', 'image/png', 1377, NULL, '2026-01-15 18:31:03.770', '2026-01-15 18:31:03.770', '/uploads/learning-materials/thumbnails/thumb-1768501863768.svg'),
(96, 'Screenshot 2026-01-15 203113', '', '/uploads/files/1768501884125-3b0pmxjn7bx.png', 'image/png', 29742, NULL, '2026-01-15 18:31:24.133', '2026-01-15 18:31:24.133', '/uploads/learning-materials/thumbnails/thumb-1768501884125.svg'),
(97, 'Screenshot 2026-01-15 203141', '', '/uploads/files/1768501912681-1st5u3vadao.png', 'image/png', 43639, NULL, '2026-01-15 18:31:52.683', '2026-01-15 18:31:52.683', '/uploads/learning-materials/thumbnails/thumb-1768501912681.svg'),
(98, 'Screenshot 2026-01-15 203204', '', '/uploads/files/1768501939681-fdo698rxwmw.png', 'image/png', 7132, NULL, '2026-01-15 18:32:19.855', '2026-01-15 18:32:19.855', '/uploads/learning-materials/thumbnails/thumb-1768501939681.svg'),
(99, 'Screenshot 2026-01-15 203227', '', '/uploads/files/1768501958143-9ualg5eicq.png', 'image/png', 22132, NULL, '2026-01-15 18:32:38.145', '2026-01-15 18:32:38.145', '/uploads/learning-materials/thumbnails/thumb-1768501958143.svg'),
(100, 'Screenshot 2026-01-15 203249', '', '/uploads/files/1768501978025-nsk65r261w.png', 'image/png', 20519, NULL, '2026-01-15 18:32:58.028', '2026-01-15 18:32:58.028', '/uploads/learning-materials/thumbnails/thumb-1768501978025.svg'),
(101, 'Screenshot 2026-01-15 203307', '', '/uploads/files/1768501995515-1m2nwsjzu0i.png', 'image/png', 21451, NULL, '2026-01-15 18:33:15.543', '2026-01-15 18:33:15.543', '/uploads/learning-materials/thumbnails/thumb-1768501995515.svg'),
(102, 'Screenshot 2026-01-15 203329', '', '/uploads/files/1768502022177-l0q1doohjx.png', 'image/png', 32215, NULL, '2026-01-15 18:33:42.179', '2026-01-15 18:33:42.179', '/uploads/learning-materials/thumbnails/thumb-1768502022177.svg'),
(103, 'Screenshot 2026-01-15 203348', '', '/uploads/files/1768502042836-37en0016y8n.png', 'image/png', 39397, NULL, '2026-01-15 18:34:02.839', '2026-01-15 18:34:02.839', '/uploads/learning-materials/thumbnails/thumb-1768502042836.svg'),
(104, 'Screenshot 2026-01-15 203410', '', '/uploads/files/1768502065517-bkv2k6qltzb.png', 'image/png', 47484, NULL, '2026-01-15 18:34:25.523', '2026-01-15 18:34:25.523', '/uploads/learning-materials/thumbnails/thumb-1768502065517.svg'),
(105, 'Screenshot 2026-01-15 203459', '', '/uploads/files/1768502112950-hbff87ugctn.png', 'image/png', 6380, NULL, '2026-01-15 18:35:13.051', '2026-01-15 18:35:13.051', '/uploads/learning-materials/thumbnails/thumb-1768502112950.svg'),
(106, 'Screenshot 2026-01-15 203522', '', '/uploads/files/1768502137182-bnh5vab6c7o.png', 'image/png', 61455, NULL, '2026-01-15 18:35:37.185', '2026-01-15 18:35:37.185', '/uploads/learning-materials/thumbnails/thumb-1768502137182.svg'),
(107, 'Screenshot 2026-01-15 203544', '', '/uploads/files/1768502174948-pyhm5v6gxp.png', 'image/png', 62769, NULL, '2026-01-15 18:36:15.078', '2026-01-15 18:36:15.078', '/uploads/learning-materials/thumbnails/thumb-1768502174948.svg'),
(108, 'Screenshot 2026-01-15 203623', '', '/uploads/files/1768502195402-ynzcs36xs5s.png', 'image/png', 53043, NULL, '2026-01-15 18:36:35.404', '2026-01-15 18:36:35.404', '/uploads/learning-materials/thumbnails/thumb-1768502195402.svg'),
(109, 'Screenshot 2026-01-15 203641', '', '/uploads/files/1768502212075-3f9in9u7je.png', 'image/png', 57799, NULL, '2026-01-15 18:36:52.079', '2026-01-15 18:36:52.079', '/uploads/learning-materials/thumbnails/thumb-1768502212075.svg'),
(110, 'Screenshot 2026-01-15 203658', '', '/uploads/files/1768502236489-bqtyyf6clvm.png', 'image/png', 69328, NULL, '2026-01-15 18:37:16.495', '2026-01-15 18:37:16.495', '/uploads/learning-materials/thumbnails/thumb-1768502236489.svg'),
(111, 'Screenshot 2026-01-15 203719', '', '/uploads/files/1768502257772-xkac7wxwozl.png', 'image/png', 42817, NULL, '2026-01-15 18:37:37.775', '2026-01-15 18:37:37.775', '/uploads/learning-materials/thumbnails/thumb-1768502257772.svg'),
(112, 'Screenshot 2026-01-15 203737', '', '/uploads/files/1768502275427-xr9wgn5r5i.png', 'image/png', 69695, NULL, '2026-01-15 18:37:55.428', '2026-01-15 18:37:55.428', '/uploads/learning-materials/thumbnails/thumb-1768502275427.svg'),
(113, 'Screenshot 2026-01-15 203802', '', '/uploads/files/1768502293713-vfna3uziay8.png', 'image/png', 51095, NULL, '2026-01-15 18:38:13.749', '2026-01-15 18:38:13.749', '/uploads/learning-materials/thumbnails/thumb-1768502293713.svg'),
(114, 'Screenshot 2026-01-15 203821', '', '/uploads/files/1768502309410-we5porytjnh.png', 'image/png', 14991, NULL, '2026-01-15 18:38:29.412', '2026-01-15 18:38:29.412', '/uploads/learning-materials/thumbnails/thumb-1768502309410.svg'),
(115, 'Screenshot 2026-01-15 203838', '', '/uploads/files/1768502332146-4ufvluqh5ec.png', 'image/png', 51496, NULL, '2026-01-15 18:38:52.149', '2026-01-15 18:38:52.149', '/uploads/learning-materials/thumbnails/thumb-1768502332146.svg'),
(116, 'Screenshot 2026-01-15 203857', '', '/uploads/files/1768502351364-o8jc6nvaqgn.png', 'image/png', 29626, NULL, '2026-01-15 18:39:11.428', '2026-01-15 18:39:11.428', '/uploads/learning-materials/thumbnails/thumb-1768502351364.svg'),
(117, 'Screenshot 2026-01-15 203915', '', '/uploads/files/1768502364553-krgejbzxykq.png', 'image/png', 29719, NULL, '2026-01-15 18:39:24.555', '2026-01-15 18:39:24.555', '/uploads/learning-materials/thumbnails/thumb-1768502364553.svg'),
(118, 'Screenshot 2026-01-15 203931', '', '/uploads/files/1768502379926-lnv5a3tyd3.png', 'image/png', 23566, NULL, '2026-01-15 18:39:39.929', '2026-01-15 18:39:39.929', '/uploads/learning-materials/thumbnails/thumb-1768502379926.svg'),
(119, 'Screenshot 2026-01-15 203954', '', '/uploads/files/1768502415662-9uqqdnjcv6d.png', 'image/png', 70337, NULL, '2026-01-15 18:40:15.781', '2026-01-15 18:40:15.781', '/uploads/learning-materials/thumbnails/thumb-1768502415662.svg'),
(120, 'Screenshot 2026-01-15 204018', '', '/uploads/files/1768502430275-fwlfoywc0i4.png', 'image/png', 57620, NULL, '2026-01-15 18:40:30.277', '2026-01-15 18:40:30.277', '/uploads/learning-materials/thumbnails/thumb-1768502430275.svg'),
(121, 'Screenshot 2026-01-15 204038', '', '/uploads/files/1768502481464-qzfbhbnsldj.png', 'image/png', 52821, NULL, '2026-01-15 18:41:21.583', '2026-01-15 18:41:21.583', '/uploads/learning-materials/thumbnails/thumb-1768502481464.svg'),
(122, 'Screenshot 2026-01-15 204126', '', '/uploads/files/1768502501003-m9mspbjkujh.png', 'image/png', 53902, NULL, '2026-01-15 18:41:41.006', '2026-01-15 18:41:41.006', '/uploads/learning-materials/thumbnails/thumb-1768502501003.svg'),
(123, 'Screenshot 2026-01-15 204146', '', '/uploads/files/1768502513988-gu3rfdugyfo.png', 'image/png', 15871, NULL, '2026-01-15 18:41:53.990', '2026-01-15 18:41:53.990', '/uploads/learning-materials/thumbnails/thumb-1768502513988.svg'),
(124, 'Screenshot 2026-01-15 204213', '', '/uploads/files/1768502546424-qawtchpe15m.png', 'image/png', 25569, NULL, '2026-01-15 18:42:26.537', '2026-01-15 18:42:26.537', '/uploads/learning-materials/thumbnails/thumb-1768502546424.svg'),
(125, 'Screenshot 2026-01-15 204302', '', '/uploads/files/1768502596551-e5tvb82itvu.png', 'image/png', 48551, NULL, '2026-01-15 18:43:16.607', '2026-01-15 18:43:16.607', '/uploads/learning-materials/thumbnails/thumb-1768502596551.svg'),
(126, 'Screenshot 2026-01-15 204428', '', '/uploads/files/1768502679503-wvi5anq9b6d.png', 'image/png', 3654, NULL, '2026-01-15 18:44:39.507', '2026-01-15 18:44:39.507', '/uploads/learning-materials/thumbnails/thumb-1768502679503.svg'),
(127, 'Screenshot 2026-01-15 204446', '', '/uploads/files/1768502693291-fhejgwycbya.png', 'image/png', 5175, NULL, '2026-01-15 18:44:53.293', '2026-01-15 18:44:53.293', '/uploads/learning-materials/thumbnails/thumb-1768502693291.svg'),
(128, 'Screenshot 2026-01-15 204505', '', '/uploads/files/1768502712419-26757mdb21p.png', 'image/png', 5059, NULL, '2026-01-15 18:45:12.432', '2026-01-15 18:45:12.432', '/uploads/learning-materials/thumbnails/thumb-1768502712419.svg'),
(129, 'Screenshot 2026-01-15 204525', '', '/uploads/files/1768502736977-mskpw4j4lml.png', 'image/png', 13394, NULL, '2026-01-15 18:45:37.056', '2026-01-15 18:45:37.056', '/uploads/learning-materials/thumbnails/thumb-1768502736977.svg'),
(130, 'Screenshot 2026-01-15 204546', '', '/uploads/files/1768502760131-ptibzkar3pj.png', 'image/png', 4766, NULL, '2026-01-15 18:46:00.134', '2026-01-15 18:46:00.134', '/uploads/learning-materials/thumbnails/thumb-1768502760131.svg'),
(131, 'Screenshot 2026-01-15 204607', '', '/uploads/files/1768502778721-8b9oa107m1u.png', 'image/png', 4775, NULL, '2026-01-15 18:46:18.730', '2026-01-15 18:46:18.730', '/uploads/learning-materials/thumbnails/thumb-1768502778721.svg'),
(132, 'Screenshot 2026-01-15 204628', '', '/uploads/files/1768502818486-zvkvrhptbcd.png', 'image/png', 6318, NULL, '2026-01-15 18:46:58.487', '2026-01-15 18:46:58.487', '/uploads/learning-materials/thumbnails/thumb-1768502818486.svg'),
(133, 'Screenshot 2026-01-15 204716', '', '/uploads/files/1768502849250-cjuwrruey7j.png', 'image/png', 5834, NULL, '2026-01-15 18:47:29.334', '2026-01-15 18:47:29.334', '/uploads/learning-materials/thumbnails/thumb-1768502849250.svg'),
(134, 'Screenshot 2026-01-15 204744', '', '/uploads/files/1768502882144-pi1scaz062r.png', 'image/png', 5108, NULL, '2026-01-15 18:48:02.146', '2026-01-15 18:48:02.146', '/uploads/learning-materials/thumbnails/thumb-1768502882144.svg'),
(135, 'Screenshot 2026-01-15 204812', '', '/uploads/files/1768502928384-6z6md0umysk.png', 'image/png', 99584, NULL, '2026-01-15 18:48:48.388', '2026-01-15 18:48:48.388', '/uploads/learning-materials/thumbnails/thumb-1768502928384.svg'),
(136, 'Screenshot 2026-01-15 204904', '', '/uploads/files/1768502982532-a7d9rasahzk.png', 'image/png', 124319, NULL, '2026-01-15 18:49:42.536', '2026-01-15 18:49:42.536', '/uploads/learning-materials/thumbnails/thumb-1768502982532.svg'),
(137, 'Screenshot 2026-01-15 204927', '', '/uploads/files/1768503016400-a4731bfm6bv.png', 'image/png', 71639, NULL, '2026-01-15 18:50:16.421', '2026-01-15 18:50:16.421', '/uploads/learning-materials/thumbnails/thumb-1768503016400.svg'),
(138, 'Screenshot 2026-01-15 205052', '', '/uploads/files/1768503076614-qbbd18t998m.png', 'image/png', 96570, NULL, '2026-01-15 18:51:16.723', '2026-01-15 18:51:16.723', '/uploads/learning-materials/thumbnails/thumb-1768503076614.svg'),
(139, 'Screenshot 2026-01-15 205141', '', '/uploads/files/1768503134675-bbp4gd3uilo.png', 'image/png', 76995, NULL, '2026-01-15 18:52:14.751', '2026-01-15 18:52:14.751', '/uploads/learning-materials/thumbnails/thumb-1768503134675.svg'),
(140, 'Screenshot 2026-01-15 205159', '', '/uploads/files/1768503181610-lsow24jmx6o.png', 'image/png', 64637, NULL, '2026-01-15 18:53:01.614', '2026-01-15 18:53:01.614', '/uploads/learning-materials/thumbnails/thumb-1768503181610.svg'),
(141, 'Screenshot 2026-01-15 205316', '', '/uploads/files/1768503209981-5cdmhyae8ku.png', 'image/png', 64726, NULL, '2026-01-15 18:53:29.990', '2026-01-15 18:53:29.990', '/uploads/learning-materials/thumbnails/thumb-1768503209981.svg'),
(142, 'Screenshot 2026-01-15 205344', '', '/uploads/files/1768503254864-h4x5og2kiou.png', 'image/png', 110533, NULL, '2026-01-15 18:54:14.942', '2026-01-15 18:54:14.942', '/uploads/learning-materials/thumbnails/thumb-1768503254864.svg'),
(143, 'Screenshot 2026-01-15 205405', '', '/uploads/files/1768503293347-ykg6q51pmw.png', 'image/png', 77793, NULL, '2026-01-15 18:54:53.349', '2026-01-15 18:54:53.349', '/uploads/learning-materials/thumbnails/thumb-1768503293347.svg'),
(144, 'Screenshot 2026-01-15 205530', '', '/uploads/files/1768503356359-bmz4o81bfvt.png', 'image/png', 61683, NULL, '2026-01-15 18:55:56.423', '2026-01-15 18:55:56.423', '/uploads/learning-materials/thumbnails/thumb-1768503356359.svg'),
(145, 'Screenshot 2026-01-15 205553', '', '/uploads/files/1768503387324-t74uvul44c.png', 'image/png', 67369, NULL, '2026-01-15 18:56:27.444', '2026-01-15 18:56:27.444', '/uploads/learning-materials/thumbnails/thumb-1768503387324.svg'),
(146, 'Screenshot 2026-01-15 205626', '', '/uploads/files/1768503745459-ziqqojk2rrb.png', 'image/png', 97412, NULL, '2026-01-15 19:02:26.060', '2026-01-15 19:02:26.060', '/uploads/learning-materials/thumbnails/thumb-1768503745459.svg'),
(147, 'Screenshot 2026-01-15 210259', '', '/uploads/files/1768503961214-ym58jn7rur8.png', 'image/png', 67030, NULL, '2026-01-15 19:06:01.250', '2026-01-15 19:06:01.250', '/uploads/learning-materials/thumbnails/thumb-1768503961214.svg'),
(148, 'Screenshot 2026-01-15 210623', '', '/uploads/files/1768504142163-k5the4wvbr.png', 'image/png', 97403, NULL, '2026-01-15 19:09:02.192', '2026-01-15 19:09:02.192', '/uploads/learning-materials/thumbnails/thumb-1768504142163.svg'),
(149, 'Screenshot 2026-01-15 210924', '', '/uploads/files/1768504176197-8enudtdntvs.png', 'image/png', 92149, NULL, '2026-01-15 19:09:36.201', '2026-01-15 19:09:36.201', '/uploads/learning-materials/thumbnails/thumb-1768504176197.svg'),
(150, 'Screenshot 2026-01-15 210951', '', '/uploads/files/1768504201167-0t2sk68mrhfp.png', 'image/png', 86026, NULL, '2026-01-15 19:10:01.170', '2026-01-15 19:10:01.170', '/uploads/learning-materials/thumbnails/thumb-1768504201167.svg'),
(151, 'Screenshot 2026-01-15 211010', '', '/uploads/files/1768504220034-29kioccrrtm.png', 'image/png', 66696, NULL, '2026-01-15 19:10:20.083', '2026-01-15 19:10:20.083', '/uploads/learning-materials/thumbnails/thumb-1768504220034.svg'),
(152, 'Screenshot 2026-01-15 211029', '', '/uploads/files/1768504243296-21ynaix1233.png', 'image/png', 14741, NULL, '2026-01-15 19:10:43.298', '2026-01-15 19:10:43.298', '/uploads/learning-materials/thumbnails/thumb-1768504243296.svg'),
(153, 'Screenshot 2026-01-15 211051', '', '/uploads/files/1768504258696-ituyyvx445.png', 'image/png', 6110, NULL, '2026-01-15 19:10:58.698', '2026-01-15 19:10:58.698', '/uploads/learning-materials/thumbnails/thumb-1768504258696.svg'),
(154, 'Screenshot 2026-01-15 211109', '', '/uploads/files/1768504281037-rtjz29q5cl.png', 'image/png', 5002, NULL, '2026-01-15 19:11:21.118', '2026-01-15 19:11:21.118', '/uploads/learning-materials/thumbnails/thumb-1768504281037.svg'),
(155, 'Screenshot 2026-01-15 211130', '', '/uploads/files/1768504296549-tvsupw63ys.png', 'image/png', 4877, NULL, '2026-01-15 19:11:36.551', '2026-01-15 19:11:36.551', '/uploads/learning-materials/thumbnails/thumb-1768504296549.svg'),
(156, 'Screenshot 2026-01-15 211159', '', '/uploads/files/1768504330023-pypdp2nes28.png', 'image/png', 17754, NULL, '2026-01-15 19:12:10.071', '2026-01-15 19:12:10.071', '/uploads/learning-materials/thumbnails/thumb-1768504330023.svg'),
(157, 'Screenshot 2026-01-15 211220', '', '/uploads/files/1768504347019-1f44z0qmyugh.png', 'image/png', 15711, NULL, '2026-01-15 19:12:27.021', '2026-01-15 19:12:27.021', '/uploads/learning-materials/thumbnails/thumb-1768504347019.svg'),
(158, 'Screenshot 2026-01-15 211242', '', '/uploads/files/1768504369029-ksc4hoo2t4.png', 'image/png', 14696, NULL, '2026-01-15 19:12:49.032', '2026-01-15 19:12:49.032', '/uploads/learning-materials/thumbnails/thumb-1768504369029.svg'),
(159, 'Screenshot 2026-01-15 211258', '', '/uploads/files/1768504388000-3uogl22vwkb.png', 'image/png', 19432, NULL, '2026-01-15 19:13:08.074', '2026-01-15 19:13:08.074', '/uploads/learning-materials/thumbnails/thumb-1768504388000.svg'),
(160, 'Screenshot 2026-01-15 211316', '', '/uploads/files/1768504405836-w91i0h6xl8i.png', 'image/png', 15672, NULL, '2026-01-15 19:13:25.838', '2026-01-15 19:13:25.838', '/uploads/learning-materials/thumbnails/thumb-1768504405836.svg'),
(161, 'Screenshot 2026-01-15 211340', '', '/uploads/files/1768504427535-60m5zzp6mkl.png', 'image/png', 18039, NULL, '2026-01-15 19:13:47.539', '2026-01-15 19:13:47.539', '/uploads/learning-materials/thumbnails/thumb-1768504427535.svg'),
(162, 'Screenshot 2026-01-15 211410', '', '/uploads/files/1768504501112-qi3k65xuki.png', 'image/png', 24827, NULL, '2026-01-15 19:15:01.117', '2026-01-15 19:15:01.117', '/uploads/learning-materials/thumbnails/thumb-1768504501112.svg'),
(163, 'Screenshot 2026-01-15 211435', '', '/uploads/files/1768504519774-n43zqjgejse.png', 'image/png', 6981, NULL, '2026-01-15 19:15:19.851', '2026-01-15 19:15:19.851', '/uploads/learning-materials/thumbnails/thumb-1768504519774.svg'),
(164, 'Screenshot 2026-01-15 211540', '', '/uploads/files/1768504560232-u8xa2d39do.png', 'image/png', 26514, NULL, '2026-01-15 19:16:00.234', '2026-01-15 19:16:00.234', '/uploads/learning-materials/thumbnails/thumb-1768504560232.svg'),
(165, 'Screenshot 2026-01-15 211616', '', '/uploads/files/1768504592489-qk3joxmen49.png', 'image/png', 5467, NULL, '2026-01-15 19:16:32.526', '2026-01-15 19:16:32.526', '/uploads/learning-materials/thumbnails/thumb-1768504592489.svg'),
(166, 'Screenshot 2026-01-15 211646', '', '/uploads/files/1768504672414-wsguiu09anm.png', 'image/png', 9588, NULL, '2026-01-15 19:17:52.418', '2026-01-15 19:17:52.418', '/uploads/learning-materials/thumbnails/thumb-1768504672414.svg'),
(167, 'Screenshot 2026-01-15 211710', '', '/uploads/files/1768504699768-h6c253b7og8.png', 'image/png', 7101, NULL, '2026-01-15 19:18:19.780', '2026-01-15 19:18:19.780', '/uploads/learning-materials/thumbnails/thumb-1768504699768.svg'),
(168, 'Screenshot 2026-01-15 211833', '', '/uploads/files/1768504721247-3yrcmevcunw.png', 'image/png', 6844, NULL, '2026-01-15 19:18:41.249', '2026-01-15 19:18:41.249', '/uploads/learning-materials/thumbnails/thumb-1768504721247.svg'),
(169, 'Screenshot 2026-01-15 211847', '', '/uploads/files/1768504733587-datj5dnpc8u.png', 'image/png', 7165, NULL, '2026-01-15 19:18:53.590', '2026-01-15 19:18:53.590', '/uploads/learning-materials/thumbnails/thumb-1768504733587.svg'),
(170, 'Screenshot 2026-01-15 211907', '', '/uploads/files/1768504757382-0yjolvxg6f9b.png', 'image/png', 7983, NULL, '2026-01-15 19:19:17.461', '2026-01-15 19:19:17.461', '/uploads/learning-materials/thumbnails/thumb-1768504757382.svg'),
(171, 'Screenshot 2026-01-15 211924', '', '/uploads/files/1768504770512-suvpw54qbq.png', 'image/png', 5896, NULL, '2026-01-15 19:19:30.520', '2026-01-15 19:19:30.520', '/uploads/learning-materials/thumbnails/thumb-1768504770512.svg'),
(172, 'Screenshot 2026-01-15 211941', '', '/uploads/files/1768504792551-g1z5l8hcx85.png', 'image/png', 26027, NULL, '2026-01-15 19:19:52.553', '2026-01-15 19:19:52.553', '/uploads/learning-materials/thumbnails/thumb-1768504792551.svg'),
(173, 'Screenshot 2026-01-15 212001', '', '/uploads/files/1768504811799-ma7mjr9h0o.png', 'image/png', 4223, NULL, '2026-01-15 19:20:11.986', '2026-01-15 19:20:11.986', '/uploads/learning-materials/thumbnails/thumb-1768504811799.svg'),
(174, 'Screenshot 2026-01-15 212014', '', '/uploads/files/1768504822993-31l9v8ouw7q.png', 'image/png', 24868, NULL, '2026-01-15 19:20:23.002', '2026-01-15 19:20:23.002', '/uploads/learning-materials/thumbnails/thumb-1768504822993.svg'),
(175, 'Screenshot 2026-01-15 212029', '', '/uploads/files/1768504835366-slgtnh1jzyl.png', 'image/png', 5303, NULL, '2026-01-15 19:20:35.369', '2026-01-15 19:20:35.369', '/uploads/learning-materials/thumbnails/thumb-1768504835366.svg'),
(176, 'Screenshot 2026-01-15 212044', '', '/uploads/files/1768504852158-rr77dpin55p.png', 'image/png', 4625, NULL, '2026-01-15 19:20:52.160', '2026-01-15 19:20:52.160', '/uploads/learning-materials/thumbnails/thumb-1768504852158.svg'),
(177, 'Screenshot 2026-01-15 212059', '', '/uploads/files/1768504874328-v8h0n1plx2a.png', 'image/png', 28680, NULL, '2026-01-15 19:21:14.436', '2026-01-15 19:21:14.436', '/uploads/learning-materials/thumbnails/thumb-1768504874328.svg'),
(178, 'Screenshot 2026-01-15 212116', '', '/uploads/files/1768504884444-iv4r1vwilt.png', 'image/png', 4727, NULL, '2026-01-15 19:21:24.446', '2026-01-15 19:21:24.446', '/uploads/learning-materials/thumbnails/thumb-1768504884444.svg'),
(179, 'Screenshot 2026-01-15 212135', '', '/uploads/files/1768504903660-rj9dh4087ki.png', 'image/png', 7455, NULL, '2026-01-15 19:21:43.663', '2026-01-15 19:21:43.663', '/uploads/learning-materials/thumbnails/thumb-1768504903660.svg'),
(180, 'Screenshot 2026-01-15 212153', '', '/uploads/files/1768504920154-78166tv93in.png', 'image/png', 12178, NULL, '2026-01-15 19:22:00.206', '2026-01-15 19:22:00.206', '/uploads/learning-materials/thumbnails/thumb-1768504920154.svg'),
(181, 'Screenshot 2026-01-15 212207', '', '/uploads/files/1768504939276-y5xecuopsrm.png', 'image/png', 12365, NULL, '2026-01-15 19:22:19.359', '2026-01-15 19:22:19.359', '/uploads/learning-materials/thumbnails/thumb-1768504939276.svg'),
(182, 'Screenshot 2026-01-15 212225', '', '/uploads/files/1768504952276-1okvgu7vnwv.png', 'image/png', 14898, NULL, '2026-01-15 19:22:32.278', '2026-01-15 19:22:32.278', '/uploads/learning-materials/thumbnails/thumb-1768504952276.svg'),
(183, 'Screenshot 2026-01-15 212245', '', '/uploads/files/1768504971691-q1kiq0tweea.png', 'image/png', 14351, NULL, '2026-01-15 19:22:51.695', '2026-01-15 19:22:51.695', '/uploads/learning-materials/thumbnails/thumb-1768504971691.svg'),
(184, 'New File', '', '/uploads/files/1768509897561-7wyygw3em9o.png', 'image/png', 33537, NULL, '2026-01-15 20:44:57.577', '2026-01-15 20:44:57.577', '/uploads/learning-materials/thumbnails/thumb-1768509897561.svg'),
(185, 'igazeti amategeko y\'umuhanda', '', '/uploads/files/1768630913972-h6643eqs0rb.pdf', 'application/pdf', 3842700, NULL, '2026-01-17 06:21:54.053', '2026-01-17 06:21:54.053', '/uploads/learning-materials/thumbnails/thumb-1768630913972.svg'),
(186, 'Igazeti Ibyapa', '', '/uploads/files/1768630986776-rdjcr731pp.pdf', 'application/pdf', 1534208, NULL, '2026-01-17 06:23:06.867', '2026-01-17 06:23:06.867', '/uploads/learning-materials/thumbnails/thumb-1768630986776.svg'),
(187, 'Igazeti Amategeko y\'umuhanda', '', '/uploads/files/1768631656325-5wd1wt0xhu3.pdf', 'application/pdf', 5766736, NULL, '2026-01-17 06:34:16.461', '2026-01-17 06:34:16.461', '/uploads/learning-materials/thumbnails/thumb-1768631656325.svg'),
(188, 'New File', '', '/uploads/files/1769273208063-cq4jubdg5pu.jpg', 'image/jpeg', 26243, NULL, '2026-01-24 16:46:48.137', '2026-01-24 16:46:48.137', '/uploads/learning-materials/thumbnails/thumb-1769273208063.svg'),
(189, 'New File', '', '/uploads/files/1769356628706-q86dcwy0xd.jpg', 'image/jpeg', 21262, NULL, '2026-01-25 15:57:09.304', '2026-01-25 15:57:09.304', '/uploads/learning-materials/thumbnails/thumb-1769356628706.svg'),
(190, 'IMG-20251218-WA0128', '', '/uploads/files/1769356811619-ib5ud5mjut.jpg', 'image/jpeg', 90005, NULL, '2026-01-25 16:00:12.396', '2026-01-25 16:00:12.396', '/uploads/learning-materials/thumbnails/thumb-1769356811619.svg'),
(191, 'IMG-20251218-WA0129', '', '/uploads/files/1769357112497-u7mip3vu04.jpg', 'image/jpeg', 28910, NULL, '2026-01-25 16:05:14.501', '2026-01-25 16:05:14.501', '/uploads/learning-materials/thumbnails/thumb-1769357112497.svg'),
(192, 'IMG-20251218-WA0130', '', '/uploads/files/1769357233408-2e0q0wbmclh.jpg', 'image/jpeg', 23338, NULL, '2026-01-25 16:07:15.246', '2026-01-25 16:07:15.246', '/uploads/learning-materials/thumbnails/thumb-1769357233408.svg'),
(193, 'IMG-20251218-WA0131', '', '/uploads/files/1769357532604-hq28e7qrl8.jpg', 'image/jpeg', 54835, NULL, '2026-01-25 16:12:14.631', '2026-01-25 16:12:14.631', '/uploads/learning-materials/thumbnails/thumb-1769357532604.svg'),
(194, 'IMG-20251218-WA0132', '', '/uploads/files/1769357717705-k9f6awwnpd9.jpg', 'image/jpeg', 104361, NULL, '2026-01-25 16:15:19.731', '2026-01-25 16:15:19.731', '/uploads/learning-materials/thumbnails/thumb-1769357717705.svg'),
(195, 'IMG-20251218-WA0133', '', '/uploads/files/1769358133195-7o40fj5lrfh.jpg', 'image/jpeg', 52097, NULL, '2026-01-25 16:22:15.199', '2026-01-25 16:22:15.199', '/uploads/learning-materials/thumbnails/thumb-1769358133195.svg'),
(196, 'IMG-20251218-WA0134', '', '/uploads/files/1769358314412-67u1tyf531a.jpg', 'image/jpeg', 18922, NULL, '2026-01-25 16:25:14.913', '2026-01-25 16:25:14.913', '/uploads/learning-materials/thumbnails/thumb-1769358314412.svg'),
(197, 'IMG-20251218-WA0135', '', '/uploads/files/1769358433006-i0j3977v758.jpg', 'image/jpeg', 27155, NULL, '2026-01-25 16:27:13.704', '2026-01-25 16:27:13.704', '/uploads/learning-materials/thumbnails/thumb-1769358433006.svg'),
(198, 'IMG-20251218-WA0136', '', '/uploads/files/1769358614195-5acxbxldsvw.jpg', 'image/jpeg', 64716, NULL, '2026-01-25 16:30:14.998', '2026-01-25 16:30:14.998', '/uploads/learning-materials/thumbnails/thumb-1769358614195.svg'),
(200, 'New File', '', '/uploads/files/1769424627201-uoaymjza8xq.jpg', 'image/jpeg', 64716, NULL, '2026-01-26 10:50:27.229', '2026-01-26 10:50:27.229', '/uploads/learning-materials/thumbnails/thumb-1769424627201.svg'),
(201, 'IMG-20251218-WA0137', '', '/uploads/files/1769424676597-z6mvd1vvdp9.jpg', 'image/jpeg', 31695, NULL, '2026-01-26 10:51:17.199', '2026-01-26 10:51:17.199', '/uploads/learning-materials/thumbnails/thumb-1769424676597.svg'),
(202, 'IMG-20251218-WA0138', '', '/uploads/files/1769424848798-0u0s25rxngbf.jpg', 'image/jpeg', 104798, NULL, '2026-01-26 10:54:09.301', '2026-01-26 10:54:09.301', '/uploads/learning-materials/thumbnails/thumb-1769424848798.svg'),
(203, 'IMG-20251218-WA0139', '', '/uploads/files/1769424972535-6bs1oy7t3j8.jpg', 'image/jpeg', 98793, NULL, '2026-01-26 10:56:13.316', '2026-01-26 10:56:13.316', '/uploads/learning-materials/thumbnails/thumb-1769424972535.svg'),
(204, 'IMG-20251218-WA0140', '', '/uploads/files/1769424987395-lzmt51zj2oq.jpg', 'image/jpeg', 23916, NULL, '2026-01-26 10:56:27.403', '2026-01-26 10:56:27.403', '/uploads/learning-materials/thumbnails/thumb-1769424987395.svg'),
(205, 'IMG-20251218-WA0141', '', '/uploads/files/1769425039902-8xexwoyffkk.jpg', 'image/jpeg', 27331, NULL, '2026-01-26 10:57:20.896', '2026-01-26 10:57:20.896', '/uploads/learning-materials/thumbnails/thumb-1769425039902.svg'),
(206, 'IMG-20251218-WA0142', '', '/uploads/files/1769425150505-aasahpa4to5.jpg', 'image/jpeg', 35026, NULL, '2026-01-26 10:59:11.109', '2026-01-26 10:59:11.109', '/uploads/learning-materials/thumbnails/thumb-1769425150505.svg'),
(207, 'IMG-20251218-WA0143', '', '/uploads/files/1769425162898-ollm8mhvsgf.jpg', 'image/jpeg', 30149, NULL, '2026-01-26 10:59:22.900', '2026-01-26 10:59:22.900', '/uploads/learning-materials/thumbnails/thumb-1769425162898.svg'),
(208, 'IMG-20251218-WA0144', '', '/uploads/files/1769425221418-vrf80oi6hx8.jpg', 'image/jpeg', 135635, NULL, '2026-01-26 11:00:21.827', '2026-01-26 11:00:21.827', '/uploads/learning-materials/thumbnails/thumb-1769425221418.svg'),
(209, 'New File', '', '/uploads/files/1771159684453-7t49wlpn7um.png', 'image/png', 45494, NULL, '2026-02-15 12:48:04.457', '2026-02-15 12:48:04.457', '/uploads/learning-materials/thumbnails/thumb-1771159684453.svg'),
(210, 'New File', '', '/uploads/files/1771160389286-5t2v7t2vg.png', 'image/png', 61833, NULL, '2026-02-15 12:59:49.289', '2026-02-15 12:59:49.289', '/uploads/learning-materials/thumbnails/thumb-1771160389286.svg'),
(211, 'New File', '', '/uploads/files/1771164488270-j4spyhvzh9.png', 'image/png', 17182, NULL, '2026-02-15 14:08:08.273', '2026-02-15 14:08:08.273', '/uploads/learning-materials/thumbnails/thumb-1771164488270.svg'),
(212, 'New File', '', '/uploads/files/1771165104331-w8k812z54cl.png', 'image/png', 8561, NULL, '2026-02-15 14:18:24.335', '2026-02-15 14:18:24.335', '/uploads/learning-materials/thumbnails/thumb-1771165104331.svg'),
(213, 'New File', '', '/uploads/files/1771165585259-kp5k4qf95up.png', 'image/png', 8301, NULL, '2026-02-15 14:26:25.262', '2026-02-15 14:26:25.262', '/uploads/learning-materials/thumbnails/thumb-1771165585259.svg'),
(214, 'New File', '', '/uploads/files/1771165798193-o691hijpzfj.png', 'image/png', 22136, NULL, '2026-02-15 14:29:58.196', '2026-02-15 14:29:58.196', '/uploads/learning-materials/thumbnails/thumb-1771165798193.svg'),
(215, 'New File', '', '/uploads/files/1771166058483-fm7y6z6qk08.png', 'image/png', 18740, NULL, '2026-02-15 14:34:18.487', '2026-02-15 14:34:18.487', '/uploads/learning-materials/thumbnails/thumb-1771166058483.svg'),
(216, 'New File', '', '/uploads/files/1771167061244-vybyz1b0fze.png', 'image/png', 17598, NULL, '2026-02-15 14:51:01.248', '2026-02-15 14:51:01.248', '/uploads/learning-materials/thumbnails/thumb-1771167061244.svg'),
(217, 'New File', '', '/uploads/files/1771178645550-5oszoe0jinq.png', 'image/png', 9232, NULL, '2026-02-15 18:04:05.554', '2026-02-15 18:04:05.554', '/uploads/learning-materials/thumbnails/thumb-1771178645550.svg');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `files`
--
ALTER TABLE `files`
  ADD PRIMARY KEY (`id`),
  ADD KEY `files_folder_id_fkey` (`folder_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `files`
--
ALTER TABLE `files`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=218;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `files`
--
ALTER TABLE `files`
  ADD CONSTRAINT `files_folder_id_fkey` FOREIGN KEY (`folder_id`) REFERENCES `folders` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
